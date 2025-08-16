package com.gamma.spool.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.config.DebugConfig;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

@SuppressWarnings("unused")
public class ConcurrentChunkTransformer implements IClassTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.CHUNK,
        Names.Targets.CHUNK_OBF);

    static final String[][] FIELD_REDIRECTIONS = {
        { "field_76636_d", "isChunkLoaded", Names.DataTypes.BOOLEAN, "isChunkLoaded",
            "L" + Names.Destinations.ATOMIC_BOOLEAN + ";" },
        { "field_76643_l", "isModified", Names.DataTypes.BOOLEAN, "isModified",
            "L" + Names.Destinations.ATOMIC_BOOLEAN + ";" },
        { "field_76644_m", "hasEntities", Names.DataTypes.BOOLEAN, "hasEntities",
            "L" + Names.Destinations.ATOMIC_BOOLEAN + ";" },
        { "field_76649_t", "queuedLightChecks", Names.DataTypes.INTEGER, "queuedLightChecks",
            "L" + Names.Destinations.ATOMIC_INTEGER + ";" },
        { "field_82912_p", "heightMapMinimum", Names.DataTypes.INTEGER, "heightMapMinimum",
            "L" + Names.Destinations.ATOMIC_INTEGER + ";" },
        { "field_76646_k", "isTerrainPopulated", Names.DataTypes.BOOLEAN, "isTerrainPopulated",
            "L" + Names.Destinations.ATOMIC_BOOLEAN + ";" },
        { "field_150814_l", "isLightPopulated", Names.DataTypes.BOOLEAN, "isLightPopulated",
            "L" + Names.Destinations.ATOMIC_BOOLEAN + ";" },
        { "field_76642_o", "sendUpdates", Names.DataTypes.BOOLEAN, "sendUpdates",
            "L" + Names.Destinations.ATOMIC_BOOLEAN + ";" } };

    static final String[][] STATIC_FIELD_REDIRECTIONS = {
        { "field_76640_a", "isLit", Names.DataTypes.BOOLEAN, "isLit", "L" + Names.Destinations.ATOMIC_BOOLEAN + ";" } };

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
            if (!cn.name.equals(Names.Targets.CHUNK) && !cn.name.equals(Names.Targets.CHUNK_OBF)
                && !cn.name.equals(Names.Destinations.CONCURRENT_CHUNK)) // Don't redirect fields inside itself.
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
                if (!init && (tNode.desc.equals(Names.Targets.CHUNK) || tNode.desc.equals(Names.Targets.CHUNK_OBF))) {
                    init = true;
                    if (DebugConfig.logASM) SpoolLogger.warn(
                        "Redirecting Chunk instantiation to ConcurrentChunk in " + transformedName + "." + mn.name);
                    mn.instructions
                        .insertBefore(tNode, new TypeInsnNode(Opcodes.NEW, Names.Destinations.CONCURRENT_CHUNK));
                    mn.instructions.remove(tNode);
                    changed = true;
                }
            } else if (node.getOpcode() == Opcodes.INVOKESPECIAL && node instanceof MethodInsnNode mNode) {
                if (mNode.name.equals(Names.Targets.INIT)) {
                    if (init
                        && (mNode.owner.equals(Names.Targets.CHUNK) || mNode.owner.equals(Names.Targets.CHUNK_OBF))) {
                        init = false;
                        if (DebugConfig.logASM) SpoolLogger.warn(
                            "Redirecting Chunk constructor to ConcurrentChunk in " + transformedName + "." + mn.name);
                        mn.instructions.insertBefore(
                            mNode,
                            new MethodInsnNode(
                                Opcodes.INVOKESPECIAL,
                                Names.Destinations.CONCURRENT_CHUNK,
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
            if (node instanceof FieldInsnNode fieldNode) {
                if (fieldNode.owner.equals(Names.Targets.CHUNK) || fieldNode.owner.equals(Names.Targets.CHUNK_OBF)) {
                    for (String[] redirect : STATIC_FIELD_REDIRECTIONS) {
                        if ((fieldNode.name.equals(redirect[0]) || fieldNode.name.equals(redirect[1]))
                            && fieldNode.desc.equals(redirect[2])) {
                            InsnList newInsns = new InsnList();

                            if (fieldNode.getOpcode() == Opcodes.GETSTATIC) {
                                if (DebugConfig.logASM) SpoolLogger.warn(
                                    "Redirecting Chunk." + fieldNode.name
                                        + " GETSTATIC call to ConcurrentChunk (atomic) in "
                                        + transformedName
                                        + "."
                                        + mn.name);
                                // Get field and call get()
                                newInsns.add(
                                    new FieldInsnNode(
                                        Opcodes.GETSTATIC,
                                        Names.Destinations.CONCURRENT_CHUNK,
                                        redirect[3],
                                        redirect[4]));
                                newInsns.add(
                                    new MethodInsnNode(
                                        Opcodes.INVOKEVIRTUAL,
                                        redirect[4].substring(1, redirect[4].length() - 1),
                                        "get",
                                        "()" + redirect[2],
                                        false));
                            } else if (fieldNode.getOpcode() == Opcodes.PUTSTATIC) {
                                if (DebugConfig.logASM) SpoolLogger.warn(
                                    "Redirecting Chunk." + fieldNode.name
                                        + " PUTSTATIC call to ConcurrentChunk (atomic) in "
                                        + transformedName
                                        + "."
                                        + mn.name);
                                newInsns.add(
                                    new FieldInsnNode(
                                        Opcodes.GETSTATIC,
                                        Names.Destinations.CONCURRENT_CHUNK,
                                        redirect[3],
                                        redirect[4]));
                                newInsns.add(new InsnNode(Opcodes.SWAP));
                                newInsns.add(
                                    new MethodInsnNode(
                                        Opcodes.INVOKEVIRTUAL,
                                        redirect[4].substring(1, redirect[4].length() - 1),
                                        "set",
                                        "(" + redirect[2] + ")V",
                                        false));
                            }

                            if (newInsns.size() != 0) {
                                mn.instructions.insertBefore(node, newInsns);
                                mn.instructions.remove(node);
                                changed = true;
                            }
                        }
                    }
                    for (String[] redirect : FIELD_REDIRECTIONS) {
                        if ((fieldNode.name.equals(redirect[0]) || fieldNode.name.equals(redirect[1]))
                            && fieldNode.desc.equals(redirect[2])) {
                            InsnList newInsns = new InsnList();

                            if (fieldNode.getOpcode() == Opcodes.GETFIELD) {
                                if (DebugConfig.logASM) SpoolLogger.warn(
                                    "Redirecting Chunk." + fieldNode.name
                                        + " GETFIELD call to ConcurrentChunk (atomic) in "
                                        + transformedName
                                        + "."
                                        + mn.name);
                                // Get field and call get()
                                newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, Names.Destinations.CONCURRENT_CHUNK));
                                newInsns.add(
                                    new FieldInsnNode(
                                        Opcodes.GETFIELD,
                                        Names.Destinations.CONCURRENT_CHUNK,
                                        redirect[3],
                                        redirect[4]));
                                newInsns.add(
                                    new MethodInsnNode(
                                        Opcodes.INVOKEVIRTUAL,
                                        redirect[4].substring(1, redirect[4].length() - 1),
                                        "get",
                                        "()" + redirect[2],
                                        false));
                            } else if (fieldNode.getOpcode() == Opcodes.PUTFIELD) {
                                if (DebugConfig.logASM) SpoolLogger.warn(
                                    "Redirecting Chunk." + fieldNode.name
                                        + " PUTFIELD call to ConcurrentChunk (atomic) in "
                                        + transformedName
                                        + "."
                                        + mn.name);
                                newInsns.add(new InsnNode(Opcodes.SWAP));
                                newInsns.add(new TypeInsnNode(Opcodes.CHECKCAST, Names.Destinations.CONCURRENT_CHUNK));
                                newInsns.add(
                                    new FieldInsnNode(
                                        Opcodes.GETFIELD,
                                        Names.Destinations.CONCURRENT_CHUNK,
                                        redirect[3],
                                        redirect[4]));
                                newInsns.add(new InsnNode(Opcodes.SWAP));
                                newInsns.add(
                                    new MethodInsnNode(
                                        Opcodes.INVOKEVIRTUAL,
                                        redirect[4].substring(1, redirect[4].length() - 1),
                                        "set",
                                        "(" + redirect[2] + ")V",
                                        false));
                            }

                            if (newInsns.size() != 0) {
                                mn.instructions.insertBefore(node, newInsns);
                                mn.instructions.remove(node);
                                changed = true;
                            }
                        }
                    }
                }
            }
        }

        return changed;
    }
}
