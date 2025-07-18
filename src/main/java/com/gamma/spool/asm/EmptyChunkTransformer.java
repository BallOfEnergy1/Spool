package com.gamma.spool.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.config.ConcurrentConfig;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

@SuppressWarnings("unused")
public class EmptyChunkTransformer implements IClassTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.EMPTY_CHUNK,
        Names.Targets.EMPTY_CHUNK_OBF);

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

        boolean changed = false;
        boolean init = false;

        // Change the superclass
        if (Names.Targets.EMPTY_CHUNK.equals(cn.name) || Names.Targets.EMPTY_CHUNK_OBF.equals(cn.name)) {
            SpoolLogger.warn("Changing EmptyChunk superclass to ConcurrentChunk");
            cn.superName = Names.Destinations.CONCURRENT_CHUNK;
            changed = true;
        }

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

        verifyTransformation(cn);

        return changed;
    }

    private void verifyTransformation(ClassNode cn) {
        if ((Names.Targets.EMPTY_CHUNK.equals(cn.name) || Names.Targets.EMPTY_CHUNK_OBF.equals(cn.name))
            && !Names.Destinations.CONCURRENT_CHUNK.equals(cn.superName)) {
            throw new IllegalStateException("Failed to transform EmptyChunk superclass");
        }
    }

    /** @return Was the class changed, `init`. */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {
        boolean changed = false;
        boolean init = false;

        if (Names.Targets.INIT.equals(mn.name)) {
            for (AbstractInsnNode node : mn.instructions.toArray()) {
                if (node.getOpcode() == Opcodes.INVOKESPECIAL && node instanceof MethodInsnNode mNode) {
                    if (Names.Targets.INIT.equals(mNode.name)
                        && (Names.Targets.CHUNK.equals(mNode.owner) || Names.Targets.CHUNK_OBF.equals(mNode.owner))) {
                        SpoolLogger.warn(
                            "Redirecting EmptyChunk constructor to use ConcurrentChunk's constructor in "
                                + transformedName
                                + "."
                                + mn.name);
                        mn.instructions.insertBefore(
                            node,
                            new MethodInsnNode(
                                Opcodes.INVOKESPECIAL,
                                Names.Destinations.CONCURRENT_CHUNK,
                                Names.Targets.INIT,
                                mNode.desc,
                                false));
                        mn.instructions.remove(node);
                        changed = true;
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
