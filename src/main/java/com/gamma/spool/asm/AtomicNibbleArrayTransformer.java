package com.gamma.spool.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.config.ConcurrentConfig;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

@SuppressWarnings("unused")
public class AtomicNibbleArrayTransformer implements IClassTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.NIBBLE,
        Names.Targets.NIBBLE_OBF);

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

        // Transform methods
        for (MethodNode mn : cn.methods) {
            // Handle constructor redirections (existing code)
            boolean[] results = transformConstructors(transformedName, mn);
            changed |= results[0];
            init |= results[1];

            // Handle field redirections
            if (!cn.name.equals(Names.Targets.NIBBLE) && !cn.name.equals(Names.Targets.NIBBLE_OBF)
                && !cn.name.equals(Names.Destinations.ATOMIC_NIBBLE)) // Don't redirect fields inside itself.
                changed |= transformFieldAccesses(transformedName, mn);
        }

        if (init) {
            throw new IllegalStateException(
                "Failed to transform " + transformedName + " due to missing constructor call");
        }

        return changed;
    }

    /** @return Was the class changed, `init` */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {
        boolean changed = false;
        boolean init = false;
        for (AbstractInsnNode node : mn.instructions.toArray()) {
            if (node.getOpcode() == Opcodes.NEW && node instanceof TypeInsnNode tNode) {
                if (!init && (tNode.desc.equals(Names.Targets.NIBBLE) || tNode.desc.equals(Names.Targets.NIBBLE_OBF))) {
                    init = true;
                    SpoolLogger.warn(
                        "Redirecting NibbleArray instantiation to AtomicNibbleArray in " + transformedName
                            + "."
                            + mn.name);
                    mn.instructions
                        .insertBefore(tNode, new TypeInsnNode(Opcodes.NEW, Names.Destinations.ATOMIC_NIBBLE));
                    mn.instructions.remove(tNode);
                    changed = true;
                }
            } else if (node.getOpcode() == Opcodes.INVOKESPECIAL && node instanceof MethodInsnNode mNode) {
                if (mNode.name.equals(Names.Targets.INIT)) {
                    if (init
                        && (mNode.owner.equals(Names.Targets.NIBBLE) || mNode.owner.equals(Names.Targets.NIBBLE_OBF))) {
                        init = false;
                        SpoolLogger.warn(
                            "Redirecting NibbleArray constructor to AtomicNibbleArray in " + transformedName
                                + "."
                                + mn.name);
                        mn.instructions.insertBefore(
                            mNode,
                            new MethodInsnNode(
                                Opcodes.INVOKESPECIAL,
                                Names.Destinations.ATOMIC_NIBBLE,
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

        for (AbstractInsnNode node : mn.instructions.toArray()) {
            if (node.getOpcode() == Opcodes.GETFIELD && node instanceof FieldInsnNode fNode) {
                if ((fNode.owner.equals(Names.Targets.NIBBLE) || fNode.owner.equals(Names.Targets.NIBBLE_OBF)
                    || fNode.owner.equals(Names.Destinations.ATOMIC_NIBBLE))
                    && (fNode.name.equals(Names.Targets.DATA_FIELD) || fNode.name.equals(Names.Targets.DATA_FIELD_OBF))
                    && fNode.desc.equals(Names.DataTypes.BYTE_ARRAY)) {

                    InsnList newInsns = new InsnList();
                    SpoolLogger.warn(
                        "Redirecting NibbleArray." + fNode.name
                            + " GETFIELD to AtomicNibbleArray."
                            + Names.Destinations.ATOMIC_DATA_FUNC
                            + "() in "
                            + transformedName
                            + "."
                            + mn.name);
                    newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, Names.Destinations.ATOMIC_NIBBLE));
                    newInsns.add(
                        new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            Names.Destinations.ATOMIC_NIBBLE,
                            Names.Destinations.ATOMIC_DATA_FUNC,
                            "()" + Names.DataTypes.BYTE_ARRAY,
                            false));
                    mn.instructions.insertBefore(node, newInsns);
                    mn.instructions.remove(node);
                    changed = true;
                }
            }
        }

        return changed;
    }
}
