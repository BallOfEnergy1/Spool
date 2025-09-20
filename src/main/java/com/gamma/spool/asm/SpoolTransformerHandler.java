package com.gamma.spool.asm;

import java.util.Arrays;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.gamma.spool.asm.interfaces.IConstructorTransformer;
import com.gamma.spool.asm.interfaces.IFieldTransformer;
import com.gamma.spool.asm.interfaces.ISuperclassTransformer;
import com.gamma.spool.asm.interfaces.ITransformer;
import com.gamma.spool.asm.transformers.AtomicNibbleArrayTransformer;
import com.gamma.spool.asm.transformers.ConcurrentAnvilChunkLoaderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkProviderTransformer;
import com.gamma.spool.asm.transformers.ConcurrentChunkTransformer;
import com.gamma.spool.asm.transformers.ConcurrentExtendedBlockStorageTransformer;
import com.gamma.spool.asm.transformers.EmptyChunkTransformer;
import com.gamma.spool.config.ConcurrentConfig;

@SuppressWarnings("unused")
public class SpoolTransformerHandler implements IClassTransformer {

    /**
     * The main list of transformers.
     */
    ITransformer[] transformers = new ITransformer[] { new AtomicNibbleArrayTransformer(),
        new ConcurrentAnvilChunkLoaderTransformer(), new ConcurrentChunkProviderTransformer(),
        new ConcurrentChunkTransformer(), new ConcurrentExtendedBlockStorageTransformer(),
        new EmptyChunkTransformer() };

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {

        if (basicClass == null) return null;

        if (!ConcurrentConfig.enableConcurrentWorldAccess) return basicClass;

        if (transformedName.contains(Names.Targets.MIXINS) || transformedName.contains(Names.Targets.ASM))
            return basicClass;

        // Get all valid transformers (transformers that would like to transform this class).
        ITransformer[] validTransformers = new ITransformer[transformers.length];
        int i = 0;
        for (ITransformer transformer : transformers) {
            if (!transformer.getTargetClasses()
                .find(basicClass, true)) continue;
            validTransformers[i] = transformer;
            i++;
        }

        if (i == 0) return basicClass;

        // Trim valid transformers array to size.
        System.arraycopy(validTransformers, 0, validTransformers = new ITransformer[i], 0, i);

        final ClassReader cr = new ClassReader(basicClass);
        final ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        boolean changed = false;

        for (ITransformer validTransformer : validTransformers) {
            if (validTransformer instanceof ISuperclassTransformer)
                changed |= ((ISuperclassTransformer) validTransformer).transformSuperclass(transformedName, cn);

            boolean isFieldTransformationAllowed = false;
            if (validTransformer instanceof IFieldTransformer) {
                String[] excludedNodes = ((IFieldTransformer) validTransformer).getExcludedClassNodes();
                if (!Arrays.asList(excludedNodes)
                    .contains(cn.name)) {
                    isFieldTransformationAllowed = true;
                }
            }

            for (MethodNode mn : cn.methods) {
                if (validTransformer instanceof IConstructorTransformer) {
                    boolean[] results = ((IConstructorTransformer) validTransformer)
                        .transformConstructors(transformedName, mn);
                    changed |= results[0];
                    if (results[1]) {
                        throw new IllegalStateException(
                            "Failed to transform " + transformedName + " due to missing constructor call");
                    }
                }
                if (isFieldTransformationAllowed) {
                    changed |= ((IFieldTransformer) validTransformer).transformFieldAccesses(transformedName, mn);
                }
            }
        }

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

}
