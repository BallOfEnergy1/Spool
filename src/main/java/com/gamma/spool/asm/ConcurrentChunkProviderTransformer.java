package com.gamma.spool.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.config.DebugConfig;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

@SuppressWarnings("unused")
public class ConcurrentChunkProviderTransformer implements IClassTransformer {

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
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return null;
        if (!cstPoolParser.find(basicClass, true)) {
            return basicClass;
        }

        final ClassReader cr = new ClassReader(basicClass);
        final ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        final boolean changed = transformClassNode(transformedName, cn);
        if (changed) {
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cn.accept(cw);

            final byte[] bytes;

            bytes = cw.toByteArray();

            ClassReader checker = new ClassReader(bytes);
            checker.accept(new CheckClassAdapter(new ClassNode()), 0);
            return bytes;
        }
        return basicClass;
    }

    private boolean transformClassNode(String transformedName, ClassNode cn) {
        if (cn == null || !ConcurrentConfig.enableConcurrentWorldAccess) {
            return false;
        }

        if (transformedName.contains(Names.Targets.MIXINS) || transformedName.contains(Names.Targets.ASM)) return false;

        boolean changed = false;
        boolean init = false;

        // Transform methods
        for (MethodNode mn : cn.methods) {
            // Handle constructor redirections (existing code)
            boolean[] results = transformConstructors(transformedName, mn);
            changed |= results[0];
            init |= results[1];

            // Handle field redirections
            changed |= transformFieldAccesses(transformedName, mn);
        }

        if (init) {
            throw new IllegalStateException(
                "Failed to transform " + transformedName + " due to missing constructor call");
        }

        return changed;
    }

    /** @return Was the class changed, `init`. */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {
        boolean changed = false;
        boolean init = false;
        for (AbstractInsnNode node : mn.instructions.toArray()) {
            if (node.getOpcode() == Opcodes.NEW && node instanceof TypeInsnNode tNode) {
                for (String[] redirect : CLASS_REDIRECTIONS) {
                    if (tNode.desc.equals(redirect[1])) {
                        init = true;
                        if (DebugConfig.logASM) SpoolLogger.warn(
                            "Redirecting " + redirect[0]
                                + " instantiation to Concurrent"
                                + redirect[0]
                                + " in "
                                + transformedName
                                + "."
                                + mn.name);
                        mn.instructions.insertBefore(tNode, new TypeInsnNode(Opcodes.NEW, redirect[2]));
                        mn.instructions.remove(tNode);
                        changed = true;
                        break;
                    }
                }
            } else if (node.getOpcode() == Opcodes.INVOKESPECIAL && node instanceof MethodInsnNode mNode) {
                if (mNode.name.equals(Names.Targets.INIT) && init) {
                    for (String[] redirect : CLASS_REDIRECTIONS) {
                        if (mNode.owner.equals(redirect[1])) {
                            init = false;
                            if (DebugConfig.logASM) SpoolLogger.warn(
                                "Redirecting " + redirect[0]
                                    + " constructor to Concurrent"
                                    + redirect[0]
                                    + " in "
                                    + transformedName
                                    + "."
                                    + mn.name);
                            mn.instructions.insertBefore(
                                mNode,
                                new MethodInsnNode(
                                    Opcodes.INVOKESPECIAL,
                                    redirect[2],
                                    Names.Targets.INIT,
                                    mNode.desc,
                                    false));
                            mn.instructions.remove(mNode);
                            break;
                        }
                    }
                }
            }
        }

        return new boolean[] { changed, init };
    }

    private boolean transformFieldAccesses(String transformedName, MethodNode mn) {
        boolean changed = false;

        // None.

        return changed;
    }
}
