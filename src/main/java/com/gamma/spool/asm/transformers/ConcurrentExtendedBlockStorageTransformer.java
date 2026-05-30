package com.gamma.spool.asm.transformers;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TypeInsnNode;

import com.gamma.gammalib.asm.BytecodeHelper;
import com.gamma.gammalib.asm.CommonNames;
import com.gamma.gammalib.asm.interfaces.IConstructorTransformer;
import com.gamma.gammalib.asm.interfaces.ISuperclassTransformer;
import com.gamma.spool.asm.SpoolNames;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class ConcurrentExtendedBlockStorageTransformer implements IConstructorTransformer, ISuperclassTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        SpoolNames.Targets.EBS,
        SpoolNames.Targets.EBS_OBF);

    @Override
    public ClassConstantPoolParser getTargetClasses() {
        return cstPoolParser;
    }

    /** @return Was the class changed, `init`. */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {

        // EndlessIDs compatibility.
        SpoolCompat.earlyInitialization();
        final String targetClass = SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.ENDLESS_IDS)
            ? SpoolNames.Destinations.CONCURRENT_EBS_EID
            : SpoolNames.Destinations.CONCURRENT_EBS;

        boolean changed = false;
        boolean init = false;

        for (AbstractInsnNode node : mn.instructions.toArray()) {

            if (!init && BytecodeHelper.canTransformInstantiation(node)) {
                TypeInsnNode typeNode = (TypeInsnNode) node;

                if (BytecodeHelper.equalsAnyString(typeNode.desc, SpoolNames.Targets.EBS, SpoolNames.Targets.EBS_OBF)) {

                    init = true;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting ExtendedBlockStorage instantiation to ConcurrentExtendedBlockStorage in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "INSTANTIATION",
                        "<init>",
                        "ExtendedBlockStorage",
                        transformedName + "." + mn.name,
                        "<init>",
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    BytecodeHelper.transformInstantiation(mn.instructions, typeNode, targetClass);

                    changed = true;
                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                if (BytecodeHelper
                    .equalsAnyString(methodNode.owner, SpoolNames.Targets.EBS, SpoolNames.Targets.EBS_OBF)) {

                    init = false;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting ExtendedBlockStorage constructor to ConcurrentExtendedBlockStorage in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "CONSTRUCTOR",
                        "<init>",
                        "ExtendedBlockStorage",
                        transformedName + "." + mn.name,
                        "<init>",
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    BytecodeHelper.transformConstructor(mn.instructions, methodNode, targetClass);
                }
            }
        }

        return new boolean[] { changed, init };
    }

    @Override
    public boolean transformSuperclass(String transformedName, ClassNode cn) {

        // EndlessIDs compatibility.
        SpoolCompat.earlyInitialization();
        boolean EIDLoaded = SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.ENDLESS_IDS);
        String targetName = (EIDLoaded ? "ConcurrentExtendedBlockStorageWrapper" : "ConcurrentExtendedBlockStorage");
        final String targetClass = EIDLoaded ? SpoolNames.Destinations.CONCURRENT_EBS_EID
            : SpoolNames.Destinations.CONCURRENT_EBS;

        if (BytecodeHelper.equalsAnyString(cn.superName, SpoolNames.Targets.EBS, SpoolNames.Targets.EBS_OBF)) {

            // Don't ASM the base classes themselves, please.
            // woe, ClassCircularityException be upon ye
            if (BytecodeHelper.equalsAnyString(
                cn.name,
                SpoolNames.Destinations.CONCURRENT_EBS_EID,
                SpoolNames.Destinations.CONCURRENT_EBS)) return false;

            SpoolLogger.asmInfo(this, "Changing " + cn.name + " superclass to " + targetName);
            SpoolCompat.logChange("SUPERCLASS", "class", cn.name, cn.name, "class", targetName);

            BytecodeHelper.replaceSuperclass(cn, targetClass);

            for (MethodNode methodNode : cn.methods) {
                if (CommonNames.INIT.equals(methodNode.name)) {
                    for (AbstractInsnNode node : methodNode.instructions) {
                        if (BytecodeHelper.canTransformConstructor(node)) {
                            MethodInsnNode methodInsnNode = (MethodInsnNode) node;
                            if (!BytecodeHelper.equalsAnyString(
                                methodInsnNode.owner,
                                SpoolNames.Targets.EBS,
                                SpoolNames.Targets.EBS_OBF)) continue;
                            BytecodeHelper.transformConstructor(methodNode.instructions, methodInsnNode, targetClass);
                        }
                    }
                }
            }

            return true;
        }
        return false;
    }
}
