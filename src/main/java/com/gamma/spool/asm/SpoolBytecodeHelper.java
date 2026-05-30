package com.gamma.spool.asm;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.TypeInsnNode;

import com.gamma.gammalib.asm.BytecodeHelper;

public class SpoolBytecodeHelper {

    public static void transformGetFieldToThreadLocal(InsnList targetList, FieldInsnNode targetNode, String newOwner,
        String newFieldName, String threadLocalType, String threadLocalName) {
        InsnList newInsns = new InsnList();

        newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, newOwner));
        newInsns.add(new FieldInsnNode(Opcodes.GETFIELD, newOwner, threadLocalName, "Ljava/util/ThreadLocal;"));
        newInsns.add(
            new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ThreadLocal", "get", "()Ljava/lang/Object;", false));
        newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, threadLocalType));
        newInsns.add(
            new MethodInsnNode(Opcodes.INVOKEVIRTUAL, threadLocalType, newFieldName, "()" + targetNode.desc, false));

        targetList.insertBefore(targetNode, newInsns);
        targetList.remove(targetNode);
    }

    public static void transformPutFieldToThreadLocal(InsnList targetList, FieldInsnNode targetNode, String newOwner,
        String newFieldName, String threadLocalType, String threadLocalName) {
        InsnList newInsns = new InsnList();

        BytecodeHelper.swap(newInsns, targetNode.desc, "L" + targetNode.owner + ";");
        newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, newOwner));
        newInsns.add(new FieldInsnNode(Opcodes.GETFIELD, newOwner, threadLocalName, "Ljava/util/ThreadLocal;"));
        newInsns.add(
            new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/ThreadLocal", "get", "()Ljava/lang/Object;", false));
        newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, threadLocalType));
        BytecodeHelper.swap(newInsns, "L" + threadLocalType + ";", targetNode.desc);
        newInsns.add(
            new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL,
                threadLocalType,
                newFieldName,
                "(" + targetNode.desc + ")V",
                false));

        targetList.insertBefore(targetNode, newInsns);
        targetList.remove(targetNode);
    }
}
