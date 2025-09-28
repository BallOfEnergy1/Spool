package com.gamma.spool.asm.transformers;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.gamma.spool.asm.BytecodeHelper;
import com.gamma.spool.asm.Names;
import com.gamma.spool.asm.interfaces.IConstructorTransformer;
import com.gamma.spool.asm.interfaces.IFieldTransformer;
import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class AtomicNibbleArrayTransformer implements IConstructorTransformer, IFieldTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.NIBBLE,
        Names.Targets.NIBBLE_OBF);

    @Override
    public ClassConstantPoolParser getTargetClasses() {
        return cstPoolParser;
    }

    /** @return Was the class changed, `init` */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {
        boolean changed = false;
        boolean init = false;
        for (AbstractInsnNode node : mn.instructions.toArray()) {
            if (!init && BytecodeHelper.canTransformInstantiation(node)) {
                TypeInsnNode typeNode = (TypeInsnNode) node;

                if (BytecodeHelper.equalsAnyString(typeNode.desc, Names.Targets.NIBBLE, Names.Targets.NIBBLE_OBF)) {

                    init = true;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray instantiation to AtomicNibbleArray in " + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformInstantiation(mn.instructions, typeNode, Names.Destinations.ATOMIC_NIBBLE);
                    changed = true;
                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                if (BytecodeHelper.equalsAnyString(methodNode.owner, Names.Targets.NIBBLE, Names.Targets.NIBBLE_OBF)) {

                    init = false;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray constructor to AtomicNibbleArray in " + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformConstructor(mn.instructions, methodNode, Names.Destinations.ATOMIC_NIBBLE);
                }
            }
        }

        return new boolean[] { changed, init };
    }

    @Override
    public String[] getExcludedClassNodes() {
        return new String[] { Names.Targets.NIBBLE, Names.Targets.NIBBLE_OBF, Names.Destinations.ATOMIC_NIBBLE };
    }

    public boolean transformFieldAccesses(String transformedName, MethodNode mn) {
        boolean changed = false;

        for (AbstractInsnNode node : mn.instructions.toArray()) {

            if (BytecodeHelper.canTransformGetField(node)) {
                FieldInsnNode fieldNode = (FieldInsnNode) node;

                if (BytecodeHelper.equalsAnyString(
                    fieldNode.owner,
                    Names.Targets.NIBBLE,
                    Names.Targets.NIBBLE_OBF,
                    Names.Destinations.ATOMIC_NIBBLE)
                    && BytecodeHelper
                        .equalsAnyString(fieldNode.name, Names.Targets.DATA_FIELD, Names.Targets.DATA_FIELD_OBF)
                    && fieldNode.desc.equals(Names.DataTypes.BYTE_ARRAY)) {

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray." + fieldNode.name
                            + " GETFIELD call to AtomicNibbleArray (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformGetFieldToAtomic(
                        mn.instructions,
                        fieldNode,
                        Names.Destinations.ATOMIC_NIBBLE,
                        Names.Destinations.ATOMIC_NIBBLE_DATA,
                        Names.Destinations.ATOMIC_REF);

                    changed = true;
                }
            } else if (BytecodeHelper.canTransformPutField(node)) {
                FieldInsnNode fieldNode = (FieldInsnNode) node;

                if (BytecodeHelper.equalsAnyString(
                    fieldNode.owner,
                    Names.Targets.NIBBLE,
                    Names.Targets.NIBBLE_OBF,
                    Names.Destinations.ATOMIC_NIBBLE)
                    && BytecodeHelper
                        .equalsAnyString(fieldNode.name, Names.Targets.DATA_FIELD, Names.Targets.DATA_FIELD_OBF)
                    && fieldNode.desc.equals(Names.DataTypes.BYTE_ARRAY)) {

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray." + fieldNode.name
                            + " PUTFIELD call to AtomicNibbleArray (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformPutFieldToAtomic(
                        mn.instructions,
                        fieldNode,
                        Names.Destinations.ATOMIC_NIBBLE,
                        Names.Destinations.ATOMIC_NIBBLE_DATA,
                        Names.Destinations.ATOMIC_REF);

                    changed = true;
                }
            }
        }

        return changed;
    }
}
