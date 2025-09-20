package com.gamma.spool.asm.transformers;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.gamma.spool.SpoolLogger;
import com.gamma.spool.asm.BytecodeHelper;
import com.gamma.spool.asm.Names;
import com.gamma.spool.asm.interfaces.IConstructorTransformer;
import com.gamma.spool.asm.interfaces.ISuperclassTransformer;
import com.gtnewhorizon.gtnhlib.asm.ClassConstantPoolParser;

public class EmptyChunkTransformer implements IConstructorTransformer, ISuperclassTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.EMPTY_CHUNK,
        Names.Targets.EMPTY_CHUNK_OBF);

    @Override
    public ClassConstantPoolParser getTargetClasses() {
        return cstPoolParser;
    }

    /** @return Was the class changed, `init`. */
    public boolean[] transformConstructors(String transformedName, MethodNode mn) {
        boolean changed = false;
        boolean init = false;

        if (Names.Targets.INIT.equals(mn.name)) {

            for (AbstractInsnNode node : mn.instructions.toArray()) {

                if (BytecodeHelper.canTransformConstructor(node)) {
                    MethodInsnNode methodNode = (MethodInsnNode) node;

                    if (BytecodeHelper
                        .equalsAnyString(methodNode.owner, Names.Targets.CHUNK, Names.Targets.CHUNK_OBF)) {

                        SpoolLogger.asmInfo(
                            this,
                            "Redirecting EmptyChunk constructor to use ConcurrentChunk's constructor in "
                                + transformedName
                                + "."
                                + mn.name);

                        BytecodeHelper
                            .transformConstructor(mn.instructions, methodNode, Names.Destinations.CONCURRENT_CHUNK);

                        changed = true;
                    }
                }
            }
        }

        return new boolean[] { changed, init };
    }

    @Override
    public boolean transformSuperclass(String transformedName, ClassNode cn) {
        if (BytecodeHelper.equalsAnyString(cn.name, Names.Targets.EMPTY_CHUNK, Names.Targets.EMPTY_CHUNK_OBF)) {

            SpoolLogger.asmInfo(this, "Changing EmptyChunk superclass to ConcurrentChunk");

            BytecodeHelper.replaceSuperclass(cn, Names.Destinations.CONCURRENT_CHUNK);

            return true;
        }
        return false;
    }
}
