package com.gamma.spool.asm.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import net.bytebuddy.jar.asm.ClassReader;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.api.statistics.ICache;
import com.gamma.spool.api.statistics.ICachedItem;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ObjectSets;

@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class ClassHierarchyUtil implements ICache {

    private static final ClassHierarchyUtil INSTANCE = new ClassHierarchyUtil();

    private final Object2ObjectMap<String, ObjectList<String>> hierarchyCache = Object2ObjectMaps
        .synchronize(new Object2ObjectOpenHashMap<>());
    private final Object2ObjectMap<String, ObjectSet<String>> interfaceCache = Object2ObjectMaps
        .synchronize(new Object2ObjectOpenHashMap<>());

    public static ClassHierarchyUtil getInstance() {
        return INSTANCE;
    }

    public String getCommonSuperClass(String class1, String class2) {
        // Contract: input and output are internal names (with '/')
        if (class1 == null || class2 == null) {
            return "java/lang/Object";
        }
        if (class1.equals(class2)) {
            return class1;
        }

        // Handle arrays per JVM rules
        if (class1.startsWith("[") || class2.startsWith("[")) {
            return getArrayCommonSuper(class1, class2);
        }

        try {
            // If class1 is a super type of class2, return class1
            if (isAssignableFrom(class2, class1)) {
                return class1;
            }
            // If class2 is a super type of class1, return class2
            if (isAssignableFrom(class1, class2)) {
                return class2;
            }

            // Build ancestors set for class1 (include itself)
            ObjectSet<String> ancestors1 = new ObjectOpenHashSet<>();
            ancestors1.add(class1);
            ObjectList<String> supers1 = hierarchyCache.computeIfAbsent(class1, this::computeSuperClasses);
            ancestors1.addAll(supers1);

            // Walk up from class2 until we find a match in ancestors1
            String current = class2;
            while (true) {
                if (ancestors1.contains(current)) {
                    return current;
                }
                ObjectList<String> supers2 = hierarchyCache.computeIfAbsent(current, this::computeSuperClasses);
                if (supers2.isEmpty()) {
                    // Reached the top or unknown; default to Object
                    return "java/lang/Object";
                }
                // Move to direct superclass (nearest parent is the last element)
                current = supers2.get(supers2.size() - 1);
            }
        } catch (Throwable t) {
            // If anything goes wrong, be conservative
            return "java/lang/Object";
        }
    }

    public boolean isAssignableFrom(String childClass, String parentClass) {
        // Contract: parameters are internal names
        if (childClass.equals(parentClass)) {
            return true;
        }

        // Direct superclasses chain contains parent
        ObjectList<String> superClasses = hierarchyCache.computeIfAbsent(childClass, this::computeSuperClasses);
        if (superClasses.contains(parentClass)) {
            return true;
        }

        // Interfaces closure contains parent
        ObjectSet<String> interfaces = interfaceCache.computeIfAbsent(childClass, this::computeInterfaces);
        return interfaces.contains(parentClass);
    }

    private ObjectList<String> computeSuperClasses(String className) {
        // Contract: internal names in and out
        ObjectList<String> superClasses = new ObjectArrayList<>();
        try {
            String currentClass = className;
            while (true) {
                ClassReader reader = getReader(currentClass);
                String superName = reader.getSuperName();
                if (superName == null || superName.equals("java/lang/Object")) {
                    break;
                }
                superClasses.add(0, superName);
                currentClass = superName;
            }
        } catch (IOException e) {
            // If we can't read the class, return empty set
            return ObjectLists.emptyList();
        }
        return ObjectLists.unmodifiable(superClasses);
    }

    private ObjectSet<String> computeInterfaces(String className) {
        // Contract: internal names in and out
        ObjectSet<String> allInterfaces = new ObjectOpenHashSet<>();
        try {
            // Get all interfaces including those from superclasses
            collectInterfaces(className, allInterfaces);
        } catch (IOException e) {
            // If we can't read the class, return empty set
            return ObjectSets.emptySet();
        }
        return ObjectSets.unmodifiable(allInterfaces);
    }

    private void collectInterfaces(String className, ObjectSet<String> interfaces) throws IOException {
        // className: internal
        ClassReader reader = getReader(className);

        // Add direct interfaces (internal names)
        String[] interfaceNames = reader.getInterfaces();
        for (String interfaceName : interfaceNames) {
            if (interfaces.add(interfaceName)) {
                // Recursively collect interfaces from the interface
                collectInterfaces(interfaceName, interfaces);
            }
        }

        // Collect interfaces from superclass
        String superName = reader.getSuperName();
        if (superName != null && !superName.equals("java/lang/Object")) {
            collectInterfaces(superName, interfaces);
        }
    }

    @Override
    public void invalidate() {
        hierarchyCache.clear();
        interfaceCache.clear();
    }

    @Override
    public ICachedItem<?>[] getCacheItems() {
        return new ICachedItem<?>[] { new ICachedItem<Object2ObjectMap<String, ObjectList<String>>>() {

            @Override
            public Object2ObjectMap<String, ObjectList<String>> getItem() {
                return hierarchyCache;
            }

            @Override
            public void setItem(Object2ObjectMap<String, ObjectList<String>> item) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long calculateSize() {
                long size = 0;
                for (Map.Entry<String, ObjectList<String>> s : hierarchyCache.object2ObjectEntrySet()) {
                    size += s.getKey()
                        .length();
                    for (String s1 : s.getValue()) {
                        size += s1.length();
                    }
                }
                return size;
            }
        }, new ICachedItem<Object2ObjectMap<String, ObjectSet<String>>>() {

            @Override
            public Object2ObjectMap<String, ObjectSet<String>> getItem() {
                return interfaceCache;
            }

            @Override
            public void setItem(Object2ObjectMap<String, ObjectSet<String>> item) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long calculateSize() {
                long size = 0;
                for (Object2ObjectMap.Entry<String, ObjectSet<String>> s : interfaceCache.object2ObjectEntrySet()) {
                    size += s.getKey()
                        .length();
                    for (String s1 : s.getValue()) {
                        size += s1.length();
                    }
                }
                return size;
            }
        } };
    }

    @Override
    public int getCount() {
        int count = 0;
        for (Map.Entry<String, ObjectList<String>> entry : hierarchyCache.entrySet()) {
            count++;
            count += entry.getValue()
                .size();
        }
        for (Map.Entry<String, ObjectSet<String>> entry : interfaceCache.entrySet()) {
            count++;
            count += entry.getValue()
                .size();
        }
        return count;
    }

    @Override
    public String getNameForDebug() {
        return "ClassHierarchyCache";
    }

    // --- Helpers for class resolution and arrays ---

    private String getArrayCommonSuper(String t1, String t2) {
        // If both are arrays
        if (t1.startsWith("[") && t2.startsWith("[")) {
            int d1 = arrayDimensions(t1);
            int d2 = arrayDimensions(t2);
            if (d1 != d2) {
                return "java/lang/Object";
            }
            String c1 = arrayComponentDesc(t1);
            String c2 = arrayComponentDesc(t2);
            // If both primitive components and equal
            if (isPrimitiveDescriptor(c1) || isPrimitiveDescriptor(c2)) {
                return Objects.equals(c1, c2) ? t1 : "java/lang/Object";
            }
            // Reference components: strip L ... ; to internal
            String i1 = c1.substring(1, c1.length() - 1);
            String i2 = c2.substring(1, c2.length() - 1);
            String lcs = getCommonSuperClass(i1, i2);
            StringBuilder sb = new StringBuilder(d1 + 2 + lcs.length());
            for (int i = 0; i < d1; i++) sb.append('[');
            sb.append('L')
                .append(lcs)
                .append(';');
            return sb.toString();
        }
        // One is array, the other is not
        return "java/lang/Object";
    }

    private int arrayDimensions(String desc) {
        int i = 0;
        while (i < desc.length() && desc.charAt(i) == '[') i++;
        return i;
    }

    private String arrayComponentDesc(String desc) {
        int i = arrayDimensions(desc);
        return desc.substring(i);
    }

    private boolean isPrimitiveDescriptor(String desc) {
        return desc.length() == 1 && "BCDFIJSZ".indexOf(desc.charAt(0)) >= 0;
    }

    private ClassReader getReader(String internalName) throws IOException {
        ClassLoader cl = Thread.currentThread()
            .getContextClassLoader();
        if (cl == null) cl = ClassHierarchyUtil.class.getClassLoader();
        String resource = internalName + ".class";
        try (InputStream is = cl.getResourceAsStream(resource)) {
            if (is != null) {
                return new ClassReader(is);
            }
        }
        // Fallback: try current class loader of this class
        ClassLoader cl2 = ClassHierarchyUtil.class.getClassLoader();
        if (cl2 != null && cl2 != cl) {
            try (InputStream is2 = cl2.getResourceAsStream(resource)) {
                if (is2 != null) return new ClassReader(is2);
            }
        }
        throw new IOException("Class bytes not found for " + internalName);
    }

    private boolean isInterfaceInternal(String internalName) {
        try {
            ClassReader r = getReader(internalName);
            int access = r.getAccess();
            return (access & 0x0200) != 0; // ACC_INTERFACE
        } catch (IOException e) {
            try {
                Class<?> cls = Class.forName(
                    internalToBinary(internalName),
                    false,
                    Thread.currentThread()
                        .getContextClassLoader());
                return cls.isInterface();
            } catch (ClassNotFoundException ex) {
                return false;
            }
        }
    }

    private String internalToBinary(String internal) {
        return internal.replace('/', '.');
    }

    private String binaryToInternal(String binary) {
        return binary.replace('.', '/');
    }
}
