package com.gamma.spool.asm;

import java.util.ArrayList;
import java.util.Arrays;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.CheckClassAdapter;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.checks.UnsafeIterationHandler;
import com.gamma.spool.asm.interfaces.ICheckTransformer;
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
import com.gamma.spool.asm.util.ClassHierarchyUtil;
import com.gamma.spool.config.CompatConfig;
import com.gamma.spool.config.ConcurrentConfig;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;

@SuppressWarnings("unused")
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class SpoolTransformerHandler implements IClassTransformer {

    private static final ObjectSet<String> processedClasses = new ObjectOpenHashSet<>();

    /**
     * The main list of transformers.
     */
    ITransformer[] transformers = new ITransformer[] { new AtomicNibbleArrayTransformer(),
        new ConcurrentAnvilChunkLoaderTransformer(), new ConcurrentChunkProviderTransformer(),
        new ConcurrentChunkTransformer(), new ConcurrentExtendedBlockStorageTransformer(),
        new EmptyChunkTransformer() };

    /**
     * The main list of check transformers.
     */
    ICheckTransformer[] checks = new ICheckTransformer[] { new UnsafeIterationHandler() };

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (processedClasses.contains(transformedName)) return basicClass;
        if (basicClass == null) {
            processedClasses.add(transformedName);
            return null;
        }
        if (!ConcurrentConfig.enableConcurrentWorldAccess) {
            processedClasses.add(transformedName);
            return basicClass;
        }

        String className = transformedName.replace(".", "/");

        if (transformedName.contains(Names.Targets.MIXINS) || transformedName.contains(Names.Targets.ASM)
            || transformedName.contains(Names.Targets.CORE)) {
            processedClasses.add(transformedName);
            return basicClass;
        }

        final ClassReader classReader = new ClassReader(basicClass);
        final ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);

        boolean changed = false;

        changed |= transformBySpool(transformedName, classNode, basicClass);
        if (CompatConfig.enableASMChecks) {
            changed |= runASMChecks(transformedName, classNode, basicClass);
        }

        if (changed) {
            ClassWriter cw = new SafeClassWriter(classReader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            classNode.accept(cw);

            final byte[] bytes;

            bytes = cw.toByteArray();

            ClassReader checker = new ClassReader(bytes);
            checker.accept(new CheckClassAdapter(new ClassNode()), 0);
            processedClasses.add(transformedName);
            return bytes;
        }
        processedClasses.add(transformedName);
        return basicClass;
    }

    public boolean transformBySpool(String transformedName, ClassNode classNode, byte[] bytecode) {

        // Get all valid transformers (transformers that would like to transform this class).
        ITransformer[] validTransformers = new ITransformer[transformers.length];
        int i = 0;
        for (ITransformer transformer : transformers) {
            if (!transformer.getTargetClasses()
                .find(bytecode, true)) continue;
            validTransformers[i] = transformer;
            i++;
        }

        if (i == 0) return false;

        // Trim valid transformers array to size.
        System.arraycopy(validTransformers, 0, validTransformers = new ITransformer[i], 0, i);

        boolean changed = false;

        for (ITransformer validTransformer : validTransformers) {
            if (validTransformer instanceof ISuperclassTransformer)
                changed |= ((ISuperclassTransformer) validTransformer).transformSuperclass(transformedName, classNode);

            boolean isFieldTransformationAllowed = false;
            if (validTransformer instanceof IFieldTransformer) {
                String[] excludedNodes = ((IFieldTransformer) validTransformer).getExcludedClassNodes();
                if (!Arrays.asList(excludedNodes)
                    .contains(classNode.name)) {
                    isFieldTransformationAllowed = true;
                }
            }

            for (MethodNode mn : classNode.methods) {
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

        return changed;
    }

    public boolean runASMChecks(String transformedName, ClassNode classNode, byte[] bytecode) {
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotationNode : classNode.visibleAnnotations) {
                if (!annotationNode.desc.equals("L" + Names.SKIP_ASM_CHECKS_ANNOTATION + ";")) continue;
                for (Object o : annotationNode.values) {
                    // Check for skip annotations.
                    if (!(o instanceof ArrayList array)) continue;
                    for (Object value : array)
                        if (value instanceof String[]strings && strings[1].equals("ALL")) return false;
                }
            }
        }

        boolean changed = false;

        try {
            for (ICheckTransformer checkTransformer : checks) {
                if (classNode.visibleAnnotations != null) {
                    for (AnnotationNode annotationNode : classNode.visibleAnnotations) {
                        if (!annotationNode.desc.equals("L" + Names.SKIP_ASM_CHECKS_ANNOTATION + ";")) continue;
                        for (Object o : annotationNode.values) {
                            // Check for skip annotations.
                            if (!(o instanceof ArrayList array)) continue;
                            for (Object value : array) if (value instanceof String[]strings
                                && strings[1].equals(checkTransformer.getAnnotationCheckName())) return false;
                        }
                    }
                }
                changed |= checkTransformer.performCheck(transformedName, classNode, bytecode);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Spool failed to perform ASM checks.", e);
        }
        // Add more checks here.

        return changed;
    }

    private static class SafeClassWriter extends ClassWriter {

        public SafeClassWriter(ClassReader classReader, int flags) {
            super(classReader, flags);
        }

        public SafeClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            return ClassHierarchyUtil.getInstance()
                .getCommonSuperClass(type1, type2);
        }
    }
}
