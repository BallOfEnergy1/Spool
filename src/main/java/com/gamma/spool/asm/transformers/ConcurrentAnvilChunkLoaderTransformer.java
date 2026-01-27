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

public class ConcurrentAnvilChunkLoaderTransformer implements IConstructorTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        SpoolNames.Targets.ANVIL_CHUNK_LOADER,
        SpoolNames.Targets.ANVIL_CHUNK_LOADER_OBF);

    @Override
    public ClassConstantPoolParser getTargetClasses() {
        return cstPoolParser;
    }

    /** @return Was the class changed, `init`. */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {

        boolean changed = false;
        boolean init = false;

        for (AbstractInsnNode node : mn.instructions.toArray()) {

            if (!init && BytecodeHelper.canTransformInstantiation(node)) {
                TypeInsnNode typeNode = (TypeInsnNode) node;

                if (BytecodeHelper.equalsAnyString(
                    typeNode.desc,
                    SpoolNames.Targets.ANVIL_CHUNK_LOADER,
                    SpoolNames.Targets.ANVIL_CHUNK_LOADER_OBF)) {

                    init = true;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting AnvilChunkLoader instantiation to ConcurrentAnvilChunkLoader in " + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "INSTANTIATION",
                        "<init>",
                        "AnvilChunkLoader",
                        transformedName + "." + mn.name,
                        "<init>",
                        "ConcurrentAnvilChunkLoader");

                    BytecodeHelper.transformInstantiation(
                        mn.instructions,
                        typeNode,
                        SpoolNames.Destinations.CONCURRENT_ANVIL_CHUNK_LOADER);

                    changed = true;
                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                if (BytecodeHelper.equalsAnyString(
                    methodNode.owner,
                    SpoolNames.Targets.ANVIL_CHUNK_LOADER,
                    SpoolNames.Targets.ANVIL_CHUNK_LOADER_OBF)) {
                    init = false;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting AnvilChunkLoader constructor to ConcurrentAnvilChunkLoader in " + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "CONSTRUCTOR",
                        "<init>",
                        "AnvilChunkLoader",
                        transformedName + "." + mn.name,
                        "<init>",
                        "ConcurrentAnvilChunkLoader");

                    BytecodeHelper.transformConstructor(
                        mn.instructions,
                        methodNode,
                        SpoolNames.Destinations.CONCURRENT_ANVIL_CHUNK_LOADER);
                }
            }
        }

        return new boolean[] { changed, init };
    }
}
