package com.gamma.spool.asm.transformers;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.gamma.spool.asm.BytecodeHelper;
import com.gamma.spool.asm.Names;
import com.gamma.spool.asm.interfaces.IConstructorTransformer;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class ConcurrentChunkProviderTransformer implements IConstructorTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.CHUNK_PROVIDER_SERVER,
        Names.Targets.CHUNK_PROVIDER_SERVER_OBF,
        Names.Targets.CHUNK_PROVIDER_CLIENT,
        Names.Targets.CHUNK_PROVIDER_CLIENT_OBF,
        Names.Targets.CHUNK_PROVIDER_FLAT,
        Names.Targets.CHUNK_PROVIDER_FLAT_OBF,
        Names.Targets.CHUNK_PROVIDER_GENERATE,
        Names.Targets.CHUNK_PROVIDER_GENERATE_OBF,
        Names.Targets.CHUNK_PROVIDER_HELL,
        Names.Targets.CHUNK_PROVIDER_HELL_OBF,
        Names.Targets.CHUNK_PROVIDER_END,
        Names.Targets.CHUNK_PROVIDER_END_OBF);

    static final String[][] CLASS_REDIRECTIONS = {
        { "ChunkProviderServer", Names.Targets.CHUNK_PROVIDER_SERVER,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_SERVER },
        { "ChunkProviderServer", Names.Targets.CHUNK_PROVIDER_SERVER_OBF,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_SERVER },
        { "ChunkProviderClient", Names.Targets.CHUNK_PROVIDER_CLIENT,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_CLIENT },
        { "ChunkProviderClient", Names.Targets.CHUNK_PROVIDER_CLIENT_OBF,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_CLIENT },
        { "ChunkProviderFlat", Names.Targets.CHUNK_PROVIDER_FLAT, Names.Destinations.CONCURRENT_CHUNK_PROVIDER_FLAT },
        { "ChunkProviderFlat", Names.Targets.CHUNK_PROVIDER_FLAT_OBF,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_FLAT },
        { "ChunkProviderGenerate", Names.Targets.CHUNK_PROVIDER_GENERATE,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_GENERATE },
        { "ChunkProviderGenerate", Names.Targets.CHUNK_PROVIDER_GENERATE_OBF,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_GENERATE },
        { "ChunkProviderHell", Names.Targets.CHUNK_PROVIDER_HELL, Names.Destinations.CONCURRENT_CHUNK_PROVIDER_HELL },
        { "ChunkProviderHell", Names.Targets.CHUNK_PROVIDER_HELL_OBF,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_HELL },
        { "ChunkProviderEnd", Names.Targets.CHUNK_PROVIDER_END, Names.Destinations.CONCURRENT_CHUNK_PROVIDER_END },
        { "ChunkProviderEnd", Names.Targets.CHUNK_PROVIDER_END_OBF,
            Names.Destinations.CONCURRENT_CHUNK_PROVIDER_END } };

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
