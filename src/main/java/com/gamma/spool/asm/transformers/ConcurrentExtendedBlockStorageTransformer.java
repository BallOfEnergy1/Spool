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

public class ConcurrentExtendedBlockStorageTransformer implements IConstructorTransformer {

    private static final ClassConstantPoolParser cstPoolParser = new ClassConstantPoolParser(
        Names.Targets.EBS,
        Names.Targets.EBS_OBF);

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

                if (BytecodeHelper.equalsAnyString(typeNode.desc, Names.Targets.EBS, Names.Targets.EBS_OBF)) {

                    init = true;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting ExtendedBlockStorage instantiation to ConcurrentExtendedBlockStorage in "
                            + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformInstantiation(mn.instructions, typeNode, Names.Destinations.CONCURRENT_EBS);

                    changed = true;
                }

            } else if (init && BytecodeHelper.canTransformConstructor(node)) {
                MethodInsnNode methodNode = (MethodInsnNode) node;

                if (BytecodeHelper.equalsAnyString(methodNode.owner, Names.Targets.EBS, Names.Targets.EBS_OBF)) {

                    init = false;

                    SpoolLogger.asmInfo(
                        this,
                        "Redirecting ExtendedBlockStorage constructor to ConcurrentExtendedBlockStorage in "
                            + transformedName
                            + "."
                            + mn.name);

                    BytecodeHelper.transformConstructor(mn.instructions, methodNode, Names.Destinations.CONCURRENT_EBS);
                }
            }
        }

        return new boolean[] { changed, init };
    }
}
