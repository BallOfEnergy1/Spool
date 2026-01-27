package com.gamma.spool.asm.transformers;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;

import com.gamma.gammalib.asm.BytecodeHelper;
import com.gamma.gammalib.asm.CommonNames;
import com.gamma.gammalib.asm.interfaces.IConstructorTransformer;
import com.gamma.gammalib.asm.interfaces.ISuperclassTransformer;
import com.gamma.spool.asm.SpoolNames;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class EmptyChunkTransformer implements IConstructorTransformer, ISuperclassTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        SpoolNames.Targets.EMPTY_CHUNK,
        SpoolNames.Targets.EMPTY_CHUNK_OBF);

    @Override
    public ClassConstantPoolParser getTargetClasses() {
        return cstPoolParser;
    }

    /** @return Was the class changed, `init`. */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {

        // EndlessIDs compatibility.
        SpoolCompat.earlyInitialization();
        final String targetClass = SpoolCompat.isModLoaded("endlessids") ? SpoolNames.Destinations.CONCURRENT_CHUNK_EID
            : SpoolNames.Destinations.CONCURRENT_CHUNK;

        boolean changed = false;
        boolean init = false;

        if (CommonNames.INIT.equals(mn.name)) {

            for (AbstractInsnNode node : mn.instructions.toArray()) {

                if (BytecodeHelper.canTransformConstructor(node)) {
                    MethodInsnNode methodNode = (MethodInsnNode) node;

                    if (BytecodeHelper
                        .equalsAnyString(methodNode.owner, SpoolNames.Targets.CHUNK, SpoolNames.Targets.CHUNK_OBF)) {

                        SpoolLogger.asmInfo(
                            this,
                            "Redirecting EmptyChunk constructor to use ConcurrentChunk's constructor in "
                                + transformedName
                                + "."
                                + mn.name);
                        SpoolCompat.logChange(
                            "CONSTRUCTOR",
                            "<init>",
                            "EmptyChunk",
                            transformedName + "." + mn.name,
                            "<init>",
                            targetClass.substring(targetClass.lastIndexOf('/') + 1));

                        BytecodeHelper.transformConstructor(mn.instructions, methodNode, targetClass);

                        changed = true;
                    }
                }
            }
        }

        return new boolean[] { changed, init };
    }

    @Override
    public boolean transformSuperclass(String transformedName, ClassNode cn) {

        // EndlessIDs compatibility.
        SpoolCompat.earlyInitialization();
        final String targetClass = SpoolCompat.isModLoaded("endlessids") ? SpoolNames.Destinations.CONCURRENT_CHUNK_EID
            : SpoolNames.Destinations.CONCURRENT_CHUNK;

        if (BytecodeHelper
            .equalsAnyString(cn.name, SpoolNames.Targets.EMPTY_CHUNK, SpoolNames.Targets.EMPTY_CHUNK_OBF)) {

            SpoolLogger.asmInfo(this, "Changing EmptyChunk superclass to ConcurrentChunk");
            SpoolCompat.logChange("SUPERCLASS", "class", "EmptyChunk", "EmptyChunk", "class", "ConcurrentChunk");

            BytecodeHelper.replaceSuperclass(cn, targetClass);

            return true;
        }
        return false;
    }
}
