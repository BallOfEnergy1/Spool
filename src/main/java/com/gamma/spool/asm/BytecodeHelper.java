package com.gamma.spool.asm;

import java.util.Objects;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Utility class for helping with bytecode transformations.
 */
public class BytecodeHelper {

    private static void swap(InsnList list, String topTypeDesc, String bottomTypeDesc) {

        Type topType = Type.getType(topTypeDesc);
        Type bottomType = Type.getType(bottomTypeDesc);

        if (topType.getSize() == 0 || bottomType.getSize() == 0) throw new IllegalStateException(
            "Cannot SWAP void value!\nTop value: " + topTypeDesc + "\nBottom value: " + bottomTypeDesc);

        if (topType.getSize() == 1) {
            if (bottomType.getSize() == 1) {
                // Both top and bottom are type-1 computational types.
                list.add(new InsnNode(Opcodes.SWAP));
            } else {
                // The top is type-1 computational type, the second is type-2.
                list.add(new InsnNode(Opcodes.DUP_X2));
                // Pop the top value off the stack.
                list.add(new InsnNode(Opcodes.POP));
            }
        } else {
            if (bottomType.getSize() == 1) {
                // The top is type-2 computational type, the second is type-1.
                list.add(new InsnNode(Opcodes.DUP2_X1));
            } else {
                // Both top and bottom are type-2 computational types.
                list.add(new InsnNode(Opcodes.DUP2_X2));
            }
            // Pop the top 2 values off (one value for type-2 computational type).
            list.add(new InsnNode(Opcodes.POP2));
        }
    }

    /**
     * Checks basic conditions for if an instantiation transformation can be applied to an instruction node.
     * 
     * @param node The node ({@link AbstractInsnNode}) to check.
     * @return true if the instantiation transformation can be applied to this node, false otherwise.
     */
    public static boolean canTransformInstantiation(AbstractInsnNode node) {
        return node != null && node.getOpcode() == Opcodes.NEW && node instanceof TypeInsnNode;
    }

    /**
     * Transforms instantiation bytecode at a certain node.
     * 
     * @param targetList The instruction list of the {@link MethodNode}.
     * @param targetNode The target node to transform.
     * @param desc       The (replacing) class to instantiate.
     */
    public static void transformInstantiation(InsnList targetList, TypeInsnNode targetNode, String desc) {
        targetList.insertBefore(targetNode, new TypeInsnNode(Opcodes.NEW, desc));
        targetList.remove(targetNode);
    }

    /**
     * Checks basic conditions for if a constructor transformation can be applied to an instruction node.
     * 
     * @param node The node ({@link AbstractInsnNode}) to check.
     * @return true if the constructor transformation can be applied to this node, false otherwise.
     */
    public static boolean canTransformConstructor(AbstractInsnNode node) {
        return node != null && node.getOpcode() == Opcodes.INVOKESPECIAL
            && node instanceof MethodInsnNode mNode
            && Names.Targets.INIT.equals(mNode.name);
    }

    /**
     * Transforms constructor bytecode at a certain node.
     * 
     * @param targetList The instruction list of the {@link MethodNode}.
     * @param targetNode The target node to transform.
     * @param desc       The (replacing) class to construct.
     */
    public static void transformConstructor(InsnList targetList, MethodInsnNode targetNode, String desc) {
        targetList.insertBefore(
            targetNode,
            new MethodInsnNode(Opcodes.INVOKESPECIAL, desc, Names.Targets.INIT, targetNode.desc, targetNode.itf));
        targetList.remove(targetNode);
    }

    /**
     * Checks basic conditions for if a get-field transformation can be applied to an instruction node.
     * 
     * @param node The node ({@link AbstractInsnNode}) to check.
     * @return true if the get-field transformation can be applied to this node, false otherwise.
     */
    public static boolean canTransformGetField(AbstractInsnNode node) {
        return node != null && node.getOpcode() == Opcodes.GETFIELD && node instanceof FieldInsnNode;
    }

    /**
     * Checks basic conditions for if a put-field transformation can be applied to an instruction node.
     * 
     * @param node The node ({@link AbstractInsnNode}) to check.
     * @return true if the put-field transformation can be applied to this node, false otherwise.
     */
    public static boolean canTransformPutField(AbstractInsnNode node) {
        return node != null && node.getOpcode() == Opcodes.PUTFIELD && node instanceof FieldInsnNode;
    }

    /**
     * Checks basic conditions for if a static get-field transformation can be applied to an instruction node.
     * 
     * @param node The node ({@link AbstractInsnNode}) to check.
     * @return true if the static get-field transformation can be applied to this node, false otherwise.
     */
    public static boolean canTransformStaticGetField(AbstractInsnNode node) {
        return node != null && node.getOpcode() == Opcodes.GETSTATIC && node instanceof FieldInsnNode;
    }

    /**
     * Checks basic conditions for if a static put-field transformation can be applied to an instruction node.
     * 
     * @param node The node ({@link AbstractInsnNode}) to check.
     * @return true if the static put-field transformation can be applied to this node, false otherwise.
     */
    public static boolean canTransformStaticPutField(AbstractInsnNode node) {
        return node != null && node.getOpcode() == Opcodes.PUTSTATIC && node instanceof FieldInsnNode;
    }

    /**
     * Transforms field accessor bytecode at a certain node.
     * <p>
     * This effectively replaces a field accessor with a function call to get the desired value.
     * This also means that the function <i>must</i> return the same data type as (or a supertype of)
     * the original field, as this function does not interact with any surrounding bytecode to
     * change their types.
     * </p>
     *
     * @param targetList       The instruction list of the {@link MethodNode}.
     * @param targetNode       The target node to transform.
     * @param newOwner         The new owner (<i>must</i> be a subclass of the currently transforming class).
     * @param accessorFunction The name of the function (in the new owner's class) to run to retrieve the desired value.
     * @param newDataType      The data type of the <code>accessorFunction</code> param.
     */
    public static void transformFieldAccessToFunction(InsnList targetList, FieldInsnNode targetNode, String newOwner,
        String accessorFunction, String newDataType) {
        InsnList newInsns = new InsnList();
        newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, newOwner));
        newInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, newOwner, accessorFunction, "()" + newDataType, false));
        targetList.insertBefore(targetNode, newInsns);
        targetList.remove(targetNode);
    }

    /**
     * Transforms get-field accessor bytecode at a certain node to an atomic function.
     *
     * <p>
     * This effectively replaces a field accessor with a function call (to the atomic get/set methods)
     * to get the desired value. This also means that the function <i>must</i> return the same data
     * type as (or a supertype of) the original field, as this function does not interact with any
     * surrounding bytecode to change their types.
     * </p>
     *
     * @param targetList     The instruction list of the {@link MethodNode}.
     * @param targetNode     The target node to transform.
     * @param newOwner       The new owner (<i>must</i> be a subclass of the currently transforming class).
     * @param newFieldName   The name of the field in the new owner to access.
     * @param atomicDataType The data type of the atomic field.
     */
    public static void transformGetFieldToAtomic(InsnList targetList, FieldInsnNode targetNode, String newOwner,
        String newFieldName, String atomicDataType) {
        InsnList newInsns = new InsnList();

        newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, newOwner));
        newInsns.add(new FieldInsnNode(Opcodes.GETFIELD, newOwner, newFieldName, "L" + atomicDataType + ";"));
        newInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, atomicDataType, "get", "()" + targetNode.desc, false));

        targetList.insertBefore(targetNode, newInsns);
        targetList.remove(targetNode);
    }

    /**
     * Transforms put-field accessor bytecode at a certain node to an atomic function.
     *
     * <p>
     * This effectively replaces a field accessor with a function call (to the atomic get/set methods)
     * to get the desired value. This also means that the function <i>must</i> return the same data
     * type as (or a supertype of) the original field, as this function does not interact with any
     * surrounding bytecode to change their types.
     * </p>
     *
     * @param targetList     The instruction list of the {@link MethodNode}.
     * @param targetNode     The target node to transform.
     * @param newOwner       The new owner (<i>must</i> be a subclass of the currently transforming class).
     * @param newFieldName   The name of the field in the new owner to access.
     * @param atomicDataType The data type of the atomic field.
     */
    public static void transformPutFieldToAtomic(InsnList targetList, FieldInsnNode targetNode, String newOwner,
        String newFieldName, String atomicDataType) {

        InsnList newInsns = new InsnList();

        swap(newInsns, targetNode.desc, "L" + targetNode.owner + ";");

        newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, newOwner));
        newInsns.add(new FieldInsnNode(Opcodes.GETFIELD, newOwner, newFieldName, "L" + atomicDataType + ";"));

        swap(newInsns, "L" + atomicDataType + ";", targetNode.desc);

        newInsns
            .add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, atomicDataType, "set", "(" + targetNode.desc + ")V", false));

        targetList.insertBefore(targetNode, newInsns);
        targetList.remove(targetNode);
    }

    /**
     * Transforms static get-field accessor bytecode at a certain node to an atomic function.
     *
     * <p>
     * This effectively replaces a field accessor with a function call (to the atomic get/set methods)
     * to get the desired value. This also means that the function <i>must</i> return the same data
     * type as (or a supertype of) the original field, as this function does not interact with any
     * surrounding bytecode to change their types.
     * </p>
     *
     * @param targetList     The instruction list of the {@link MethodNode}.
     * @param targetNode     The target node to transform.
     * @param newOwner       The new owner (<i>must</i> be a subclass of the currently transforming class).
     * @param newFieldName   The name of the field in the new owner to access.
     * @param atomicDataType The data type of the atomic field.
     */
    public static void transformStaticGetFieldToAtomic(InsnList targetList, FieldInsnNode targetNode, String newOwner,
        String newFieldName, String atomicDataType) {

        InsnList newInsns = new InsnList();

        newInsns.add(new FieldInsnNode(Opcodes.GETSTATIC, newOwner, newFieldName, "L" + atomicDataType + ";"));
        newInsns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, atomicDataType, "get", "()" + targetNode.desc, false));

        targetList.insertBefore(targetNode, newInsns);
        targetList.remove(targetNode);
    }

    /**
     * Transforms static put-field accessor bytecode at a certain node to an atomic function.
     *
     * <p>
     * This effectively replaces a field accessor with a function call (to the atomic get/set methods)
     * to get the desired value. This also means that the function <i>must</i> return the same data
     * type as (or a supertype of) the original field, as this function does not interact with any
     * surrounding bytecode to change their types.
     * </p>
     *
     * @param targetList     The instruction list of the {@link MethodNode}.
     * @param targetNode     The target node to transform.
     * @param newOwner       The new owner (<i>must</i> be a subclass of the currently transforming class).
     * @param newFieldName   The name of the field in the new owner to access.
     * @param atomicDataType The data type of the atomic field.
     */
    public static void transformStaticPutFieldToAtomic(InsnList targetList, FieldInsnNode targetNode, String newOwner,
        String newFieldName, String atomicDataType) {

        InsnList newInsns = new InsnList();

        newInsns.add(new FieldInsnNode(Opcodes.GETSTATIC, newOwner, newFieldName, "L" + atomicDataType + ";"));

        swap(newInsns, targetNode.desc, "L" + atomicDataType + ";");

        newInsns
            .add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, atomicDataType, "set", "(" + targetNode.desc + ")V", false));

        targetList.insertBefore(targetNode, newInsns);
        targetList.remove(targetNode);
    }

    /**
     * Replaces the superclass of a class node.
     * 
     * @param classNode     The {@link ClassNode} of which to change the superclass.
     * @param newSuperclass The new superclass.
     */
    public static void replaceSuperclass(ClassNode classNode, String newSuperclass) {
        classNode.superName = newSuperclass;
    }

    /**
     * Checks if the provided string matches any string from the given array of strings.
     *
     * @param stringToMatch The string to compare.
     * @param otherStrings  Array of strings to compare with.
     * @return true if the string to match equals any of the strings in the array, false otherwise.
     */
    public static boolean equalsAnyString(String stringToMatch, String... otherStrings) {
        for (String otherString : otherStrings) {
            if (Objects.equals(stringToMatch, otherString)) return true;
        }
        return false;
    }
}
