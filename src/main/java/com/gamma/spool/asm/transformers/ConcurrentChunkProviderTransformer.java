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

public class ConcurrentChunkProviderTransformer implements IConstructorTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        SpoolNames.Targets.CHUNK_PROVIDER_SERVER,
        SpoolNames.Targets.CHUNK_PROVIDER_SERVER_OBF,
        SpoolNames.Targets.CHUNK_PROVIDER_CLIENT,
        SpoolNames.Targets.CHUNK_PROVIDER_CLIENT_OBF,
        SpoolNames.Targets.CHUNK_PROVIDER_FLAT,
        SpoolNames.Targets.CHUNK_PROVIDER_FLAT_OBF,
        SpoolNames.Targets.CHUNK_PROVIDER_GENERATE,
        SpoolNames.Targets.CHUNK_PROVIDER_GENERATE_OBF,
        SpoolNames.Targets.CHUNK_PROVIDER_HELL,
        SpoolNames.Targets.CHUNK_PROVIDER_HELL_OBF,
        SpoolNames.Targets.CHUNK_PROVIDER_END,
        SpoolNames.Targets.CHUNK_PROVIDER_END_OBF);

    static final String[][] CLASS_REDIRECTIONS = {
        { "ChunkProviderServer", SpoolNames.Targets.CHUNK_PROVIDER_SERVER,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_SERVER },
        { "ChunkProviderServer", SpoolNames.Targets.CHUNK_PROVIDER_SERVER_OBF,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_SERVER },
        { "ChunkProviderClient", SpoolNames.Targets.CHUNK_PROVIDER_CLIENT,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_CLIENT },
        { "ChunkProviderClient", SpoolNames.Targets.CHUNK_PROVIDER_CLIENT_OBF,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_CLIENT },
        { "ChunkProviderFlat", SpoolNames.Targets.CHUNK_PROVIDER_FLAT,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_FLAT },
        { "ChunkProviderFlat", SpoolNames.Targets.CHUNK_PROVIDER_FLAT_OBF,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_FLAT },
        { "ChunkProviderGenerate", SpoolNames.Targets.CHUNK_PROVIDER_GENERATE,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_GENERATE },
        { "ChunkProviderGenerate", SpoolNames.Targets.CHUNK_PROVIDER_GENERATE_OBF,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_GENERATE },
        { "ChunkProviderHell", SpoolNames.Targets.CHUNK_PROVIDER_HELL,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_HELL },
        { "ChunkProviderHell", SpoolNames.Targets.CHUNK_PROVIDER_HELL_OBF,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_HELL },
        { "ChunkProviderEnd", SpoolNames.Targets.CHUNK_PROVIDER_END,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_END },
        { "ChunkProviderEnd", SpoolNames.Targets.CHUNK_PROVIDER_END_OBF,
            SpoolNames.Destinations.CONCURRENT_CHUNK_PROVIDER_END } };

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

                for (String[] redirect : CLASS_REDIRECTIONS) {

                    if (typeNode.desc.equals(redirect[1])) {

                        init = true;

                        SpoolLogger.asmInfo(
                            this,
                            "Redirecting " + redirect[0]
                                + " instantiation to Concurrent"
                                + redirect[0]
                                + " in "
                                + transformedName
                                + "."
                                + mn.name);
                        SpoolCompat.logChange(
                            "INSTANTIATION",
                            "<init>",
                            redirect[0],
                            transformedName + "." + mn.name,
                            "<init>",
                            "Concurrent" + redirect[0]);

                        BytecodeHelper.transformInstantiation(mn.instructions, typeNode, redirect[2]);

                        changed = true;
                        break;
                    }
                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                for (String[] redirect : CLASS_REDIRECTIONS) {

                    if (methodNode.owner.equals(redirect[1])) {

                        init = false;

                        SpoolLogger.asmInfo(
                            this,
                            "Redirecting " + redirect[0]
                                + " constructor to Concurrent"
                                + redirect[0]
                                + " in "
                                + transformedName
                                + "."
                                + mn.name);
                        SpoolCompat.logChange(
                            "CONSTRUCTOR",
                            "<init>",
                            redirect[0],
                            transformedName + "." + mn.name,
                            "<init>",
                            "Concurrent" + redirect[0]);

                        BytecodeHelper.transformConstructor(mn.instructions, methodNode, redirect[2]);

                        break;
                    }
                }
            }
        }

        return new boolean[] { changed, init };
    }
}
