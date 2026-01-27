package com.gamma.spool.asm.checks;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.LocalVariableNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TryCatchBlockNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;

import com.gamma.gammalib.asm.CommonNames;
import com.gamma.gammalib.asm.interfaces.ICheckTransformer;
import com.gamma.gammalib.asm.util.ClassHierarchyUtil;
import com.gamma.gammalib.asm.util.MethodInformation;
import com.gamma.gammalib.asm.util.ParallelSupplier;
import com.gamma.gammalib.asm.util.PhaseChain;
import com.gamma.gammalib.asm.util.StackRebuilder;
import com.gamma.gammalib.asm.util.StackView;
import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class UnsafeIterationHandler implements ICheckTransformer {

    public static final IteratorTarget[] ITERATOR_TARGETS = {

        // Compressed (field)
        new IteratorTarget(
            new String[] { "net/minecraft/world/World", "ahb" },
            new String[] { "Ljava/util/List;" },
            new String[] { "playerEntities", "field_73010_i", "h", "loadedEntityList", "field_72996_f", "e",
                "unloadedEntityList", "field_72997_g", "f", "loadedTileEntityList", "field_147482_g", "g",
                "addedTileEntityList", "field_147484_a", "a", "field_147483_b", "b", "weatherEffects", "field_73007_j",
                "i", "worldAccesses", "field_73021_x", "u" }),
        new IteratorTarget(
            new String[] { "net/minecraft/client/multiplayer/WorldClient", "bjf" },
            new String[] { "Ljava/util/Set;" },
            new String[] { "entitySpawnQueue", "field_73036_L", "K" }),
        // Compressed (field)
        new IteratorTarget(
            new String[] { "net/minecraft/entity/player/EntityPlayerMP", "mw" },
            new String[] { "Ljava/util/List;" },
            new String[] { "loadedChunks", "field_71129_f", "f", "destroyedItemsNetCache", "field_71130_g", "bN" }),
        // Compressed (field)
        new IteratorTarget(
            new String[] { "net/minecraft/server/management/PlayerManager", "mq" },
            new String[] { "Ljava/util/List;" },
            new String[] { "chunkWatcherWithPlayers", "field_72697_d", "e", "playerInstanceList", "field_111193_e",
                "f" }) };

    public static final ClassConstantPoolParser cstPoolParserClass = new ClassConstantPoolParser();

    public static final ClassConstantPoolParser cstPoolParserField = new ClassConstantPoolParser();

    static {
        // Compute class constant pool parsers.
        for (IteratorTarget iteratorTarget : ITERATOR_TARGETS) {
            for (String className : iteratorTarget.classNames) {
                // Add all target class names to the cstPoolParserClass.
                cstPoolParserClass.addString(className);
            }
            for (String fieldName : iteratorTarget.fieldNames) {
                // Add all target field names to the cstPoolParserField.
                cstPoolParserField.addString(fieldName);
            }
        }
    }

    @Override
    public ClassConstantPoolParser getTargetClasses() {
        return cstPoolParserClass;
    }

    @Override
    public String getAnnotationCheckName() {
        return "UNSAFE_ITERATION";
    }

    private static final ParallelSupplier<PhaseChain<AbstractInsnNode>> ITERATOR_PHASE_SUPPLIER = new ParallelSupplier<>(
        () -> new PhaseChain<AbstractInsnNode>()
            .nextPhase(
                (phase, node) -> node instanceof MethodInsnNode methodInsnNode
                    && methodInsnNode.desc.equals("()Ljava/util/Iterator;")
                    && methodInsnNode.name.equals("iterator"))
            .nextPhase((phase, node) -> node instanceof LabelNode)
            .nextPhase(
                (phase, node) -> node instanceof MethodInsnNode methodInsnNode
                    && methodInsnNode.desc.equals("()" + CommonNames.DataTypes.BOOLEAN)
                    && methodInsnNode.owner.equals("java/util/Iterator")
                    && methodInsnNode.name.equals("hasNext"))
            .nextPhase(
                (phase, node) -> node instanceof JumpInsnNode jumpInsnNode && jumpInsnNode.getOpcode() == Opcodes.IFEQ)
            .nextPhase(
                (phase, node) -> node instanceof MethodInsnNode methodInsnNode
                    && methodInsnNode.owner.equals("java/util/Iterator")
                    && methodInsnNode.name.equals("next"))
            .nextPhase((phase, node) -> {
                LabelNode labelNode = (LabelNode) phase.getChain()
                    .getSuccessfulInput(1);
                return node instanceof JumpInsnNode jumpInsnNode && jumpInsnNode.label.getLabel()
                    .toString()
                    .equals(
                        labelNode.getLabel()
                            .toString());
            })
            .nextPhase((phase, node) -> {
                JumpInsnNode exitJumpNode = (JumpInsnNode) phase.getChain()
                    .getSuccessfulInput(3);
                return node instanceof LabelNode labelNode && exitJumpNode.label.getLabel()
                    .toString()
                    .equals(
                        labelNode.getLabel()
                            .toString());
            }));

    private static final ParallelSupplier<PhaseChain<AbstractInsnNode>> ITERATOR_SOURCE_PHASE_SUPPLIER = new ParallelSupplier<>(
        () -> new PhaseChain<AbstractInsnNode>().nextPhase((phase, node) -> {
            if (node instanceof FieldInsnNode) {
                phase.getChain()
                    .finish(); // End the chain here for `FieldInsnNode`s.
                return true;
            } else if (node instanceof MethodInsnNode) {
                phase.getChain()
                    .setCantFindResult();
                return false;
            }
            return node instanceof VarInsnNode;
        })
            .nextPhaseAnchor(
                (phase, node) -> node instanceof VarInsnNode varInsnNode && varInsnNode.getOpcode() == Opcodes.ASTORE
                    && ((VarInsnNode) phase.getPrevious()
                        .getSuccessfulInput()).var == varInsnNode.var)
            .nextPhaseAnchor(true, (phase, node) -> {
                if (node.getOpcode() == Opcodes.DUP) return false;
                if (!(node instanceof FieldInsnNode)) phase.getChain()
                    .setCantFindResult();
                return true;
            }));

    public boolean performCheck(String transformedName, final ClassNode classNode, byte[] bytecode) {

        // Check if the class is incompatible with this check.
        if (testIsClassIncompatible(classNode)) return false;

        // Check if any target of this check is found in the class's constant pool.
        if (!testIsAnyTargetAccessedInClass(bytecode)) return false;

        final AtomicInteger count = new AtomicInteger(0);

        ObjectList<MethodInformation> methods = new ObjectArrayList<>();
        // Map the method to a standard method descriptor for proper use.
        for (MethodNode methodNode : classNode.methods) {
            methods.add(new MethodInformation(methodNode, classNode, mapLocals(methodNode)));
        }

        // This is the big part.
        methods.stream()
            // Grab all iterators and their descriptors in the method.
            .flatMap(UnsafeIterationHandler::findAllIterators)
            // Filter to only iterators that don't have an existing `synchronized` before them
            .filter(UnsafeIterationHandler::testIsUnsafeIteratorNode)
            // Filter to only iterators that need to be synchronized
            .filter(UnsafeIterationHandler::testIsIteratorTargetUnsafe)
            // Log all iteration changes
            .peek(UnsafeIterationHandler::logInjection)
            // Increment counter to ensure the returned modified flag is correct.
            .peek((node) -> count.incrementAndGet())
            // Finally, inject the synchronization
            .forEach(UnsafeIterationHandler::injectSynchronization);

        return count.get() > 0;
    }

    private static boolean testIsClassIncompatible(ClassNode classNode) {
        // Filter out classes built before Java 7.
        // Classes compiled before Java 7 do not contain stack map frames, breaking the stack rebuilder
        // and requires intensive code flow analysis which Spool cannot do yet.
        if (classNode.version < Opcodes.V1_7) return true;

        // Can't support Java > 26.
        if (classNode.version > Opcodes.V26) return false;
        return false;
    }

    private static Stream<IteratorDescriptor> findAllIterators(MethodInformation methodInfo) {
        PhaseChain<AbstractInsnNode> iteratorChain = ITERATOR_PHASE_SUPPLIER.get();
        ObjectList<IteratorDescriptor> descriptorList = new ObjectArrayList<>();

        InsnList list = methodInfo.methodNode().instructions;

        StackRebuilder rebuilder = new StackRebuilder(methodInfo, list);

        // Allows for support of nested iterators.
        int startIndex = -1;

        AbstractInsnNode[] array = list.toArray();
        for (int i = 0; i < array.length; i++) {
            AbstractInsnNode node = array[i];
            if (iteratorChain.next(node)) {
                descriptorList.add(
                    new IteratorDescriptor(
                        rebuilder,
                        methodInfo,
                        iteratorChain.getFirst()
                            .getSuccessfulInput(),
                        iteratorChain.getSuccessfulInput(3),
                        iteratorChain.getLast()
                            .getSuccessfulInput()));
                iteratorChain.reset();
                i = startIndex + 1;
                startIndex = -1;
                continue;
            }
            if (startIndex == -1 && iteratorChain.getSuccessfulInput(0) != null) startIndex = list.indexOf(node);
        }
        return descriptorList.stream();
    }

    private static boolean testIsAnyTargetAccessedInClass(byte[] bytecode) {
        return cstPoolParserClass.find(bytecode, false) && cstPoolParserField.find(bytecode, false);
    }

    private static Int2ObjectMap<LocalVariableNode> mapLocals(MethodNode methodNode) {
        Int2ObjectMap<LocalVariableNode> methodLocalMap = new Int2ObjectOpenHashMap<>();

        // Map the locals list to a map for later use.
        if (methodNode.localVariables != null) {
            for (LocalVariableNode varNode : methodNode.localVariables) {
                methodLocalMap.put(varNode.index, varNode);
            }
        }

        return methodLocalMap;
    }

    private static boolean testIsUnsafeIteratorNode(IteratorDescriptor iterator) {

        AbstractInsnNode last = iterator.start();

        if (last == null) return false;

        // Preliminary check to see if the class contains any try-catch blocks.
        // Java is required to have a try-catch block around all synchronized blocks.
        if (iterator.methodInfo()
            .methodNode().tryCatchBlocks.isEmpty()) return true;

        // Preliminary check to see if the class contains any `MONITORENTER` instructions.
        InsnList list = iterator.methodInfo()
            .methodNode().instructions;

        boolean hasMonitorInsn = false;
        for (int idx = 0; idx < list.size(); idx++) {
            // MONITORENTER cannot exist without MONITOREXIT.
            if (list.get(idx)
                .getOpcode() == Opcodes.MONITORENTER) {
                hasMonitorInsn = true;
                break;
            }
        }

        if (!hasMonitorInsn) return true;

        while (last != null) {
            last = last.getPrevious();
            if (!(last instanceof InsnNode lastInsnNode)) continue;
            if (lastInsnNode.getOpcode() != Opcodes.MONITORENTER) continue;

            StackView view = iterator.rebuilder()
                .getStackAtMethod(lastInsnNode);

            if (view == null || view.size() < 1) return false; // Don't inject if it couldn't rebuild the stack.

            if (view.popFromStack()
                .equals(((MethodInsnNode) iterator.start()).desc.substring(2))) {
                return false;
            }

            AbstractInsnNode next = iterator.end();
            while (next != null) {
                next = next.getNext();
                if (!(next instanceof InsnNode nextInsnNode)) continue;
                if (nextInsnNode.getOpcode() != Opcodes.MONITOREXIT) continue;

                view = iterator.rebuilder()
                    .getStackAtMethod(nextInsnNode);

                if (view == null || view.size() < 1) return false; // Don't inject if it couldn't rebuild the stack.

                if (view.popFromStack()
                    .equals(((MethodInsnNode) iterator.start()).desc.substring(2))) return false;
            }
        }

        return true;
    }

    private static boolean testIsIteratorTargetUnsafe(IteratorDescriptor iteratorDesc) {
        AbstractInsnNode startIteratorNode = iteratorDesc.start();

        PhaseChain<AbstractInsnNode> iteratorSourceChain = ITERATOR_SOURCE_PHASE_SUPPLIER.get();

        InsnList list = iteratorDesc.methodInfo()
            .methodNode().instructions;

        IteratorTarget target = null;

        for (int i = list.indexOf(startIteratorNode); i > 0; i--) {
            AbstractInsnNode node = list.get(i);
            if (iteratorSourceChain.next(node)) {
                if (iteratorSourceChain.cantFindResult()) {
                    return false;
                }

                AbstractInsnNode finalNode = iteratorSourceChain.getLast()
                    .getSuccessfulInput();

                FieldInsnNode fieldInsnNode = (FieldInsnNode) (finalNode == null ? iteratorSourceChain.getFirst()
                    .getSuccessfulInput() : finalNode);

                target = new IteratorTarget(fieldInsnNode.owner, fieldInsnNode.desc, fieldInsnNode.name);
                break;
            }
        }

        if (target == null) {
            return false; // If it couldn't be found.
        }

        for (IteratorTarget iteratorTarget : ITERATOR_TARGETS) {
            if (iteratorTarget.equals(target)) return true;
        }

        return false;
    }

    private static void logInjection(IteratorDescriptor iterDesc) {
        SpoolCompat.logChange(
            "CHECK",
            "ASM unsafe iterator checks",
            "Found unsafe iterator in method " + iterDesc.methodInfo()
                .methodNode().name
                + " in class "
                + iterDesc.methodInfo()
                    .classNode().name
                + ". Injecting synchronization via ASM.");
        SpoolLogger.info(
            "Found unsafe iterator in method " + iterDesc.methodInfo()
                .methodNode().name
                + " in class "
                + iterDesc.methodInfo()
                    .classNode().name
                + ". Injecting synchronization via ASM.");
    }

    private static void injectSynchronization(IteratorDescriptor iteratorDesc) {
        final MethodNode methodNode = iteratorDesc.methodInfo()
            .methodNode();
        AbstractInsnNode startIteratorNode = iteratorDesc.start();
        Int2ObjectMap<LocalVariableNode> localsMap = iteratorDesc.methodInfo()
            .localsMap();

        int localIndex = methodNode.maxLocals; // Be safe and make sure we don't override any locals (bad).
        while (localsMap.containsKey(localIndex)) {
            localIndex++; // This will be set to the lowest unused local value.
        }

        int monitorIndexErr = localIndex;
        do {
            monitorIndexErr++; // This will be set to the lowest unused local value (after `monitorIndex`)
        } while (localsMap.containsKey(monitorIndexErr));

        // Create all labels first
        LabelNode localStartLabel = new LabelNode();
        LabelNode monitorEnterLabelNode = new LabelNode();
        LabelNode iteratorExitLabelNode = new LabelNode();
        LabelNode exitLabelNode = new LabelNode();
        LabelNode handlerLabelNode = new LabelNode();
        LabelNode endLabelNode = new LabelNode();

        InsnList injectedBefore = new InsnList();

        injectedBefore.add(localStartLabel);
        injectedBefore.add(new VarInsnNode(Opcodes.ASTORE, localIndex));
        injectedBefore.add(new VarInsnNode(Opcodes.ALOAD, localIndex));
        injectedBefore.add(new InsnNode(Opcodes.MONITORENTER));
        injectedBefore.add(monitorEnterLabelNode);
        injectedBefore.add(new VarInsnNode(Opcodes.ALOAD, localIndex));

        AbstractInsnNode endIteratorNode = iteratorDesc.end();

        InsnList injectedAfter = new InsnList();

        injectedAfter.add(iteratorExitLabelNode);
        injectedAfter.add(new VarInsnNode(Opcodes.ALOAD, localIndex));
        injectedAfter.add(new InsnNode(Opcodes.MONITOREXIT));
        injectedAfter.add(exitLabelNode);
        injectedAfter.add(new JumpInsnNode(Opcodes.GOTO, endLabelNode));

        // Exception handler
        injectedAfter.add(handlerLabelNode);
        injectedAfter.add(new VarInsnNode(Opcodes.ASTORE, monitorIndexErr));
        injectedAfter.add(new VarInsnNode(Opcodes.ALOAD, localIndex));
        injectedAfter.add(new InsnNode(Opcodes.MONITOREXIT));
        injectedAfter.add(new VarInsnNode(Opcodes.ALOAD, monitorIndexErr));
        injectedAfter.add(new InsnNode(Opcodes.ATHROW));
        injectedAfter.add(endLabelNode);

        TryCatchBlockNode tryCatchBlockNode = new TryCatchBlockNode(
            monitorEnterLabelNode,
            exitLabelNode,
            handlerLabelNode,
            null);

        synchronized (methodNode) {
            // Inject before iterator
            methodNode.instructions.insertBefore(startIteratorNode, injectedBefore);
            // Inject after iterator
            methodNode.instructions.insertBefore(endIteratorNode, injectedAfter);

            // Fix Java optimization for combined labels for multiple sequential comparisons.
            // Effectively takes the comparison node for the iterator and replaces it to
            // point to one of our labels.
            AbstractInsnNode previousNode = iteratorDesc.comparisonNode();
            methodNode.instructions
                .insert(previousNode, new JumpInsnNode(previousNode.getOpcode(), iteratorExitLabelNode));
            methodNode.instructions.remove(previousNode);

            // Add exception handler to method
            methodNode.tryCatchBlocks.add(tryCatchBlockNode);

            // Add local variables.
            methodNode.localVariables.add(
                new LocalVariableNode(
                    "spool$unsafeItr$throwable",
                    "Ljava/lang/Throwable;",
                    null,
                    handlerLabelNode,
                    endLabelNode,
                    monitorIndexErr));

            methodNode.localVariables.add(
                new LocalVariableNode(
                    "spool$unsafeItr$toIterate",
                    "Ljava/util/List;",
                    null,
                    localStartLabel,
                    endLabelNode,
                    localIndex));
        }

        // CLEANUP
        iteratorDesc.rebuilder()
            .cleanCache();
    }

    @Desugar
    public record IteratorTarget(List<String> classNames, List<String> fieldTypes, List<String> fieldNames) {

        public IteratorTarget(String[] classNames, String[] fieldTypes, String[] fieldNames) {
            this(ObjectArrayList.of(classNames), ObjectArrayList.of(fieldTypes), ObjectArrayList.of(fieldNames));
        }

        public IteratorTarget(String classNames, String fieldTypes, String fieldNames) {
            this(ObjectArrayList.of(classNames), ObjectArrayList.of(fieldTypes), ObjectArrayList.of(fieldNames));
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (!(obj instanceof IteratorTarget other)) return false;
            if (other == this) return true;

            if (classNames.stream()
                .noneMatch(other.classNames::contains)) {
                for (int i = 0; i < classNames.size(); i++) {
                    String className = classNames.get(i);
                    List<String> names = other.classNames;
                    for (int j = 0; j < names.size(); j++) {
                        String otherClassName = names.get(j);
                        boolean isAssignable = ClassHierarchyUtil.getInstance()
                            .isAssignableFrom(otherClassName, className);
                        if (!isAssignable) return false;
                    }
                }
            }

            return fieldTypes.stream()
                .anyMatch(other.fieldTypes::contains)
                && fieldNames.stream()
                    .anyMatch(other.fieldNames::contains);
        }
    }

    @Desugar
    public record IteratorDescriptor(StackRebuilder rebuilder, MethodInformation methodInfo, AbstractInsnNode start,
        AbstractInsnNode comparisonNode, AbstractInsnNode end) {}
}
