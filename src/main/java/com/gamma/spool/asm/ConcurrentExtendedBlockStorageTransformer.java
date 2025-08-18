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
public class ConcurrentExtendedBlockStorageTransformer implements IClassTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.EBS,
        Names.Targets.EBS_OBF);

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
                if (!init && (tNode.desc.equals(Names.Targets.EBS) || tNode.desc.equals(Names.Targets.EBS_OBF))) {
                    init = true;
                    if (DebugConfig.logASM) SpoolLogger.warn(
                        "Redirecting ExtendedBlockStorage instantiation to ConcurrentExtendedBlockStorage in "
                            + transformedName
                            + "."
                            + mn.name);
                    mn.instructions
                        .insertBefore(tNode, new TypeInsnNode(Opcodes.NEW, Names.Destinations.CONCURRENT_EBS));
                    mn.instructions.remove(tNode);
                    changed = true;
                }
            } else if (node.getOpcode() == Opcodes.INVOKESPECIAL && node instanceof MethodInsnNode mNode) {
                if (mNode.name.equals(Names.Targets.INIT)) {
                    if (init && (mNode.owner.equals(Names.Targets.EBS) || mNode.owner.equals(Names.Targets.EBS_OBF))) {
                        init = false;
                        if (DebugConfig.logASM) SpoolLogger.warn(
                            "Redirecting ExtendedBlockStorage constructor to ConcurrentExtendedBlockStorage in "
                                + transformedName
                                + "."
                                + mn.name);
                        mn.instructions.insertBefore(
                            mNode,
                            new MethodInsnNode(
                                Opcodes.INVOKESPECIAL,
                                Names.Destinations.CONCURRENT_EBS,
                                Names.Targets.INIT,
                                mNode.desc,
                                false));
                        mn.instructions.remove(mNode);
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
