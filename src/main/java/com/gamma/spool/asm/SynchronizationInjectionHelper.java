package com.gamma.spool.asm;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.LocalVariableNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TryCatchBlockNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;

import com.gamma.spool.asm.checks.UnsafeIterationHandler;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

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
}
