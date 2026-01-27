package com.gamma.spool.asm.transformers;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TypeInsnNode;

import com.gamma.gammalib.asm.BytecodeHelper;
import com.gamma.gammalib.asm.CommonNames;
import com.gamma.gammalib.asm.interfaces.IConstructorTransformer;
import com.gamma.gammalib.asm.interfaces.IFieldTransformer;
import com.gamma.spool.asm.SpoolNames;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class AtomicNibbleArrayTransformer implements IConstructorTransformer, IFieldTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        SpoolNames.Targets.NIBBLE,
        SpoolNames.Targets.NIBBLE_OBF);

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

                if (BytecodeHelper
                    .equalsAnyString(typeNode.desc, SpoolNames.Targets.NIBBLE, SpoolNames.Targets.NIBBLE_OBF)) {

                    init = true;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray instantiation to AtomicNibbleArray in " + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "INSTANTIATION",
                        "<init>",
                        "NibbleArray",
                        transformedName + "." + mn.name,
                        "<init>",
                        "AtomicNibbleArray");

                    BytecodeHelper
                        .transformInstantiation(mn.instructions, typeNode, SpoolNames.Destinations.ATOMIC_NIBBLE);
                    changed = true;
                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                if (BytecodeHelper
                    .equalsAnyString(methodNode.owner, SpoolNames.Targets.NIBBLE, SpoolNames.Targets.NIBBLE_OBF)) {

                    init = false;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray constructor to AtomicNibbleArray in " + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "CONSTRUCTOR",
                        "<init>",
                        "NibbleArray",
                        transformedName + "." + mn.name,
                        "<init>",
                        "AtomicNibbleArray");

                    BytecodeHelper
                        .transformConstructor(mn.instructions, methodNode, SpoolNames.Destinations.ATOMIC_NIBBLE);
                }
            }
        }

        return new boolean[] { changed, init };
    }

    @Override
    public String[] getExcludedClassNodes() {
        return new String[] { SpoolNames.Targets.NIBBLE, SpoolNames.Targets.NIBBLE_OBF,
            SpoolNames.Destinations.ATOMIC_NIBBLE };
    }

    public boolean transformFieldAccesses(String transformedName, MethodNode mn) {
        boolean changed = false;

        for (AbstractInsnNode node : mn.instructions.toArray()) {

            if (BytecodeHelper.canTransformGetField(node)) {
                FieldInsnNode fieldNode = (FieldInsnNode) node;

                if (BytecodeHelper.equalsAnyString(
                    fieldNode.owner,
                    SpoolNames.Targets.NIBBLE,
                    SpoolNames.Targets.NIBBLE_OBF,
                    SpoolNames.Destinations.ATOMIC_NIBBLE)
                    && BytecodeHelper.equalsAnyString(
                        fieldNode.name,
                        SpoolNames.Targets.DATA_FIELD,
                        SpoolNames.Targets.DATA_FIELD_OBF,
                        SpoolNames.Targets.DATA_FIELD_OBF_MC)
                    && fieldNode.desc.equals(CommonNames.DataTypes.BYTE_ARRAY)) {

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray." + fieldNode.name
                            + " GETFIELD call to AtomicNibbleArray (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "GETFIELD",
                        fieldNode.name,
                        "NibbleArray",
                        transformedName + "." + mn.name,
                        SpoolNames.Destinations.ATOMIC_NIBBLE_DATA,
                        "AtomicNibbleArray");

                    BytecodeHelper.transformGetFieldToAtomic(
                        mn.instructions,
                        fieldNode,
                        SpoolNames.Destinations.ATOMIC_NIBBLE,
                        SpoolNames.Destinations.ATOMIC_NIBBLE_DATA,
                        CommonNames.ATOMIC_REF);

                    changed = true;
                }
            } else if (BytecodeHelper.canTransformPutField(node)) {
                FieldInsnNode fieldNode = (FieldInsnNode) node;

                if (BytecodeHelper.equalsAnyString(
                    fieldNode.owner,
                    SpoolNames.Targets.NIBBLE,
                    SpoolNames.Targets.NIBBLE_OBF,
                    SpoolNames.Destinations.ATOMIC_NIBBLE)
                    && BytecodeHelper.equalsAnyString(
                        fieldNode.name,
                        SpoolNames.Targets.DATA_FIELD,
                        SpoolNames.Targets.DATA_FIELD_OBF,
                        SpoolNames.Targets.DATA_FIELD_OBF_MC)
                    && fieldNode.desc.equals(CommonNames.DataTypes.BYTE_ARRAY)) {

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting NibbleArray." + fieldNode.name
                            + " PUTFIELD call to AtomicNibbleArray (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "PUTFIELD",
                        fieldNode.name,
                        "NibbleArray",
                        transformedName + "." + mn.name,
                        SpoolNames.Destinations.ATOMIC_NIBBLE_DATA,
                        "AtomicNibbleArray");

                    BytecodeHelper.transformPutFieldToAtomic(
                        mn.instructions,
                        fieldNode,
                        SpoolNames.Destinations.ATOMIC_NIBBLE,
                        SpoolNames.Destinations.ATOMIC_NIBBLE_DATA,
                        CommonNames.ATOMIC_REF);

                    changed = true;
                }
            }
        }

        return changed;
    }
}
