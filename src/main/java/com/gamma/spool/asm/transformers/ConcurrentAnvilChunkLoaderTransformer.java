package com.gamma.spool.asm.transformers;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.asm.BytecodeHelper;
import com.gamma.spool.asm.Names;
import com.gamma.spool.asm.interfaces.IConstructorTransformer;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class ConcurrentAnvilChunkLoaderTransformer implements IConstructorTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.ANVIL_CHUNK_LOADER,
        Names.Targets.ANVIL_CHUNK_LOADER_OBF);

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
                    Names.Targets.ANVIL_CHUNK_LOADER,
                    Names.Targets.ANVIL_CHUNK_LOADER_OBF)) {

                    init = true;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting AnvilChunkLoader instantiation to ConcurrentAnvilChunkLoader in " + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformInstantiation(
                        mn.instructions,
                        typeNode,
                        Names.Destinations.CONCURRENT_ANVIL_CHUNK_LOADER);

                    changed = true;
                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                if (BytecodeHelper.equalsAnyString(
                    methodNode.owner,
                    Names.Targets.ANVIL_CHUNK_LOADER,
                    Names.Targets.ANVIL_CHUNK_LOADER_OBF)) {
                    init = false;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting AnvilChunkLoader constructor to ConcurrentAnvilChunkLoader in " + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformConstructor(
                        mn.instructions,
                        methodNode,
                        Names.Destinations.CONCURRENT_ANVIL_CHUNK_LOADER);
                }
            }
        }

        return new boolean[] { changed, init };
    }
}
