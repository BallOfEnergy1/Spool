package com.gamma.spool.asm.transformers;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TypeInsnNode;

import com.gamma.gammalib.asm.BytecodeHelper;
import com.gamma.gammalib.asm.interfaces.IConstructorTransformer;
import com.gamma.spool.asm.SpoolNames;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class ConcurrentExtendedBlockStorageTransformer implements IConstructorTransformer {

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
        final String targetClass = SpoolCompat.isModLoaded("endlessids") ? SpoolNames.Destinations.CONCURRENT_EBS_EID
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
}
