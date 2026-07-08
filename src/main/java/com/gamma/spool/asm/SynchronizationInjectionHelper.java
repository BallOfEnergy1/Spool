package com.gamma.spool.asm;

import java.io.IOException;
import java.util.ArrayList;

import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.LocalVariableNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TryCatchBlockNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.obfuscation.mapping.mcp.MappingFieldSrg;
import org.spongepowered.asm.util.Bytecode;

import com.gamma.gammalib.asm.util.ObfuscatedClassHandler;
import com.gamma.spool.asm.checks.UnsafeIterationHandler;
import com.gamma.spool.core.SpoolCoreMod;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class SynchronizationInjectionHelper {

    public static void injectSynchronization(UnsafeIterationHandler.IteratorDescriptor iteratorDesc) {
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

    public static void onMixinPostApply(ClassNode classNode, IMixinInfo mixinInfo) {
        // Process methods.
        for (MethodNode mixinMethodNode : mixinInfo.getClassNode(0).methods) {
            for (MethodNode targetMethodNode : classNode.methods) {
                if (!mixinMethodNode.name.equals(targetMethodNode.name)
                    || !mixinMethodNode.desc.equals(targetMethodNode.desc)) continue;
                String[] fieldDescriptors = hasSynchronizeAnnotation(mixinMethodNode);
                if (fieldDescriptors == null) continue;
                for (String fieldDescriptor : fieldDescriptors) {
                    if (fieldDescriptor == null || fieldDescriptor.isEmpty()) continue;
                    injectSynchronization(classNode, targetMethodNode, fieldDescriptor);
                }
            }
        }
    }

    private static String[] hasSynchronizeAnnotation(MethodNode methodNode) {
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode annotationNode : methodNode.visibleAnnotations) {
                if ("Lcom/gamma/spool/api/annotations/Synchronize;".equals(annotationNode.desc)) {
                    return getAnnotationValue(annotationNode);
                }
            }
        }
        return null;
    }

    private static String[] getAnnotationValue(AnnotationNode annotationNode) {
        if (annotationNode.values == null) {
            return null;
        }
        for (int i = 0; i < annotationNode.values.size(); i += 2) {
            if ("on".equals(annotationNode.values.get(i))) {
                // noinspection unchecked
                return ((ArrayList<String>) annotationNode.values.get(i + 1)).toArray(new String[0]);
            }
        }
        return null;
    }

    private static boolean isFieldStatic(String ownerClass, String fieldName) {
        try {
            ClassReader reader = new ClassReader(ownerClass);
            ClassNode cn = new ClassNode();
            reader.accept(cn, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (FieldNode fieldNode : cn.fields) {
                if (fieldNode.name.equals(fieldName)) return Bytecode.isStatic(fieldNode);
            }
            throw new IllegalStateException("Field not found: " + fieldName);
        } catch (IOException e) {
            throw new RuntimeException("Could not read class: " + ownerClass, e);
        }
    }

    private static void injectSynchronization(ClassNode classNode, MethodNode targetNode, String fieldDescriptor) {
        InsnList before = new InsnList();
        InsnList after = new InsnList();

        // Labels for the synchronized block
        LabelNode startLabel = new LabelNode();
        LabelNode monitorEnterLabel = new LabelNode();
        LabelNode monitorEndLabel = new LabelNode();
        LabelNode handlerLabel = new LabelNode();
        LabelNode afterEndLabel = new LabelNode();

        // Allocate local variables for the monitor object and exception
        int monitorLocal = targetNode.maxLocals;
        targetNode.maxLocals++;
        int exceptionLocal = targetNode.maxLocals;
        targetNode.maxLocals++;

        // Parse field descriptor: "Lowner/class;fieldName:Lfield/type;"
        int firstSemicolon = fieldDescriptor.indexOf(';');
        if (firstSemicolon == -1) {
            return; // Invalid format
        }

        String fieldType;
        if (!fieldDescriptor.equals("this")) {
            String ownerClass = fieldDescriptor.substring(1, firstSemicolon); // Remove leading 'L'
            String srgOwnerClass = ownerClass;
            // we have to reobfuscate this :fear:
            if (SpoolCoreMod.isObfuscatedEnv) {
                String obfName = ObfuscatedClassHandler.classSRGToObf(ownerClass);
                ownerClass = obfName == null ? ownerClass : obfName;
            }

            String remainder = fieldDescriptor.substring(firstSemicolon + 1);
            int colon = remainder.indexOf(':');
            if (colon == -1) {
                return; // Invalid format
            }

            String fieldName = remainder.substring(0, colon);
            String staticCheckFieldName = fieldName;
            fieldType = remainder.substring(colon + 1);
            if (SpoolCoreMod.isObfuscatedEnv) {
                // String obfName = ObfuscatedClassHandler.classSRGToObf(fieldType);
                // fieldType = obfName == null ? fieldType : obfName;

                // integrate this into gammalib eventually lmao
                MappingFieldSrg obfName = ObfuscatedClassHandler.fieldMCPToSRG(srgOwnerClass, fieldName);
                fieldName = obfName == null ? fieldName : obfName.getSimpleName();

                obfName = ObfuscatedClassHandler.fieldMCPToObf(srgOwnerClass, fieldName);
                staticCheckFieldName = obfName == null ? staticCheckFieldName : obfName.getSimpleName();
            }

            // Non-static fields only work on the current class, static fields work with any owner class.
            before.add(startLabel);

            if (isFieldStatic(ownerClass, staticCheckFieldName))
                before.add(new FieldInsnNode(Opcodes.GETSTATIC, srgOwnerClass, fieldName, fieldType)); // get static
                                                                                                       // field
            else {
                before.add(new VarInsnNode(Opcodes.ALOAD, 0)); // get this
                before.add(new FieldInsnNode(Opcodes.GETFIELD, srgOwnerClass, fieldName, fieldType)); // get field on
                                                                                                      // this
            }
        } else {
            fieldType = "L" + classNode.name + ";";
            before.add(new VarInsnNode(Opcodes.ALOAD, 0)); // get this
        }

        before.add(new InsnNode(Opcodes.DUP));
        before.add(new VarInsnNode(Opcodes.ASTORE, monitorLocal)); // store monitor object
        // before.add(new VarInsnNode(Opcodes.ALOAD, monitorLocal)); // load monitor object
        before.add(new InsnNode(Opcodes.MONITORENTER)); // enter monitor
        before.add(monitorEnterLabel);

        // Exception handler
        after.add(monitorEndLabel);
        after.add(new JumpInsnNode(Opcodes.GOTO, afterEndLabel));
        after.add(handlerLabel);
        after.add(new VarInsnNode(Opcodes.ASTORE, exceptionLocal));
        after.add(new VarInsnNode(Opcodes.ALOAD, monitorLocal));
        after.add(new InsnNode(Opcodes.MONITOREXIT));
        after.add(new VarInsnNode(Opcodes.ALOAD, exceptionLocal));
        after.add(new InsnNode(Opcodes.ATHROW));
        after.add(afterEndLabel);

        // Add try-catch block
        TryCatchBlockNode tryCatch = new TryCatchBlockNode(monitorEnterLabel, monitorEndLabel, handlerLabel, null);
        targetNode.tryCatchBlocks.add(tryCatch);

        // Add local variable annotations for debugging
        if (targetNode.localVariables == null) {
            targetNode.localVariables = new ObjectArrayList<>();
        }
        targetNode.localVariables
            .add(new LocalVariableNode("spool$sync$monitor", fieldType, null, startLabel, afterEndLabel, monitorLocal));
        targetNode.localVariables.add(
            new LocalVariableNode(
                "spool$sync$exception",
                "Ljava/lang/Throwable;",
                null,
                handlerLabel,
                afterEndLabel,
                exceptionLocal));

        // Set the method instructions
        AbstractInsnNode node = targetNode.instructions.getFirst();
        if (node == null) throw new IllegalArgumentException(
            "Method " + targetNode.name + " in " + classNode.name + " has no instructions (abstract)!");
        targetNode.instructions.insertBefore(node, before);
        targetNode.instructions.forEach((insnNode) -> {
            if (insnNode.getOpcode() == Opcodes.RETURN || insnNode.getOpcode() == Opcodes.ARETURN
                || insnNode.getOpcode() == Opcodes.IRETURN
                || insnNode.getOpcode() == Opcodes.FRETURN
                || insnNode.getOpcode() == Opcodes.DRETURN
                || insnNode.getOpcode() == Opcodes.LRETURN) {
                InsnList exit = new InsnList();
                exit.add(new VarInsnNode(Opcodes.ALOAD, monitorLocal));
                exit.add(new InsnNode(Opcodes.MONITOREXIT));
                targetNode.instructions.insertBefore(insnNode, exit);
            }
        });
        targetNode.instructions.add(after);

        targetNode.maxStack += 3;
    }
}
