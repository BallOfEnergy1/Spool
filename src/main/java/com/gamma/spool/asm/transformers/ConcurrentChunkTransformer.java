package com.gamma.spool.asm.transformers;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.TypeInsnNode;

import com.gamma.gammalib.asm.BytecodeHelper;
import com.gamma.gammalib.asm.CommonNames;
import com.gamma.gammalib.asm.interfaces.IConstructorTransformer;
import com.gamma.gammalib.asm.interfaces.IFieldTransformer;
import com.gamma.spool.asm.SpoolNames;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.core.SpoolLogger;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class ConcurrentChunkTransformer implements IConstructorTransformer, IFieldTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        SpoolNames.Targets.CHUNK,
        SpoolNames.Targets.CHUNK_OBF);

    static final String[][] FIELD_REDIRECTIONS = { { "isChunkLoaded", "isChunkLoaded", CommonNames.ATOMIC_BOOLEAN },
        { "field_76636_d", "field_76636_d", CommonNames.ATOMIC_BOOLEAN },
        { "d", "field_76636_d", CommonNames.ATOMIC_BOOLEAN },

        { "isModified", "isModified", CommonNames.ATOMIC_BOOLEAN },
        { "field_76643_l", "field_76643_l", CommonNames.ATOMIC_BOOLEAN },
        { "n", "field_76643_l", CommonNames.ATOMIC_BOOLEAN },

        { "hasEntities", "hasEntities", CommonNames.ATOMIC_BOOLEAN },
        { "field_76644_m", "field_76644_m", CommonNames.ATOMIC_BOOLEAN },
        { "o", "field_76644_m", CommonNames.ATOMIC_BOOLEAN },

        { "queuedLightChecks", "queuedLightChecks", CommonNames.ATOMIC_INTEGER },
        { "field_76649_t", "field_76649_t", CommonNames.ATOMIC_INTEGER },
        { "x", "field_76649_t", CommonNames.ATOMIC_INTEGER },

        { "heightMapMinimum", "heightMapMinimum", CommonNames.ATOMIC_INTEGER },
        { "field_82912_p", "field_82912_p", CommonNames.ATOMIC_INTEGER },
        { "r", "field_82912_p", CommonNames.ATOMIC_INTEGER },

        { "isTerrainPopulated", "isTerrainPopulated", CommonNames.ATOMIC_BOOLEAN },
        { "field_76646_k", "field_76646_k", CommonNames.ATOMIC_BOOLEAN },
        { "k", "field_76646_k", CommonNames.ATOMIC_BOOLEAN },

        { "isLightPopulated", "isLightPopulated", CommonNames.ATOMIC_BOOLEAN },
        { "field_150814_l", "field_150814_l", CommonNames.ATOMIC_BOOLEAN },
        { "l", "field_150814_l", CommonNames.ATOMIC_BOOLEAN },

        { "lastSaveTime", "lastSaveTime", CommonNames.ATOMIC_LONG },
        { "field_76641_n", "field_76641_n", CommonNames.ATOMIC_LONG },
        { "p", "field_76641_n", CommonNames.ATOMIC_LONG },

        { "sendUpdates", "sendUpdates", CommonNames.ATOMIC_BOOLEAN },
        { "field_76642_o", "field_76642_o", CommonNames.ATOMIC_BOOLEAN },
        { "q", "field_76642_o", CommonNames.ATOMIC_BOOLEAN } };

    static final String[][] STATIC_FIELD_REDIRECTIONS = { { "isLit", "isLit", CommonNames.ATOMIC_BOOLEAN },
        { "a", "isLit", CommonNames.ATOMIC_BOOLEAN } };

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

        for (AbstractInsnNode node : mn.instructions.toArray()) {

            if (!init && BytecodeHelper.canTransformInstantiation(node)) {
                TypeInsnNode typeNode = (TypeInsnNode) node;

                if (BytecodeHelper
                    .equalsAnyString(typeNode.desc, SpoolNames.Targets.CHUNK, SpoolNames.Targets.CHUNK_OBF)) {

                    init = true;
                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting Chunk instantiation to ConcurrentChunk in " + transformedName + "." + mn.name);
                    SpoolCompat.logChange(
                        "INSTANTIATION",
                        "<init>",
                        "Chunk",
                        transformedName + "." + mn.name,
                        "<init>",
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    BytecodeHelper.transformInstantiation(mn.instructions, typeNode, targetClass);

                    changed = true;

                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                if (BytecodeHelper
                    .equalsAnyString(methodNode.owner, SpoolNames.Targets.CHUNK, SpoolNames.Targets.CHUNK_OBF)) {

                    init = false;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting Chunk constructor to ConcurrentChunk in " + transformedName + "." + mn.name);
                    SpoolCompat.logChange(
                        "CONSTRUCTOR",
                        "<init>",
                        "Chunk",
                        transformedName + "." + mn.name,
                        "<init>",
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    BytecodeHelper.transformConstructor(mn.instructions, methodNode, targetClass);
                }
            }
        }

        return new boolean[] { changed, init };
    }

    @Override
    public String[] getExcludedClassNodes() {
        return new String[] { SpoolNames.Targets.CHUNK, SpoolNames.Targets.CHUNK_OBF,
            SpoolNames.Destinations.CONCURRENT_CHUNK, SpoolNames.Destinations.CONCURRENT_CHUNK_EID };
    }

    public boolean transformFieldAccesses(String transformedName, MethodNode mn) {

        // EndlessIDs compatibility.
        SpoolCompat.earlyInitialization();
        final String targetClass = SpoolCompat.isModLoaded("endlessids") ? SpoolNames.Destinations.CONCURRENT_CHUNK_EID
            : SpoolNames.Destinations.CONCURRENT_CHUNK;

        boolean changed = false;

        for (AbstractInsnNode node : mn.instructions.toArray()) {

            if (!(node instanceof FieldInsnNode fieldNode)) {
                continue; // literally just to get rid of stupid warnings.
            } else if (node instanceof FieldInsnNode && !BytecodeHelper
                .equalsAnyString(fieldNode.owner, SpoolNames.Targets.CHUNK, SpoolNames.Targets.CHUNK_OBF)) {
                    continue;
                }

            if (BytecodeHelper.canTransformStaticGetField(node)) {

                for (String[] redirect : STATIC_FIELD_REDIRECTIONS) {
                    if (!fieldNode.name.equals(redirect[0])) {
                        continue;
                    }

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting Chunk." + fieldNode.name
                            + " GETSTATIC call to ConcurrentChunk (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "GETSTATIC",
                        fieldNode.name,
                        "Chunk",
                        transformedName + "." + mn.name,
                        redirect[1],
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    // Get field and call get()

                    BytecodeHelper.transformStaticGetFieldToAtomic(
                        mn.instructions,
                        fieldNode,
                        targetClass,
                        redirect[1],
                        redirect[2]);

                    changed = true;
                    break;
                }

            } else if (BytecodeHelper.canTransformStaticPutField(node)) {

                for (String[] redirect : STATIC_FIELD_REDIRECTIONS) {
                    if (!fieldNode.name.equals(redirect[0])) {
                        continue;
                    }

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting Chunk." + fieldNode.name
                            + " PUTSTATIC call to ConcurrentChunk (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "PUTSTATIC",
                        fieldNode.name,
                        "Chunk",
                        transformedName + "." + mn.name,
                        redirect[1],
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    BytecodeHelper.transformStaticPutFieldToAtomic(
                        mn.instructions,
                        fieldNode,
                        targetClass,
                        redirect[1],
                        redirect[2]);

                    changed = true;
                    break;
                }

            } else if (BytecodeHelper.canTransformGetField(node)) {

                for (String[] redirect : FIELD_REDIRECTIONS) {
                    if (!fieldNode.name.equals(redirect[0])) {
                        continue;
                    }

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting Chunk." + fieldNode.name
                            + " GETFIELD call to ConcurrentChunk (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "GETFIELD",
                        fieldNode.name,
                        "Chunk",
                        transformedName + "." + mn.name,
                        redirect[1],
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    BytecodeHelper
                        .transformGetFieldToAtomic(mn.instructions, fieldNode, targetClass, redirect[1], redirect[2]);

                    changed = true;
                    break;
                }

            } else if (BytecodeHelper.canTransformPutField(node)) {

                for (String[] redirect : FIELD_REDIRECTIONS) {
                    if (!fieldNode.name.equals(redirect[0])) {
                        continue;
                    }

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting Chunk." + fieldNode.name
                            + " PUTFIELD call to ConcurrentChunk (atomic) in "
                            + transformedName
                            + "."
                            + mn.name);
                    SpoolCompat.logChange(
                        "PUTFIELD",
                        fieldNode.name,
                        "Chunk",
                        transformedName + "." + mn.name,
                        redirect[1],
                        targetClass.substring(targetClass.lastIndexOf('/') + 1));

                    BytecodeHelper
                        .transformPutFieldToAtomic(mn.instructions, fieldNode, targetClass, redirect[1], redirect[2]);

                    changed = true;
                    break;
                }
            }
        }

        return changed;
    }
}
