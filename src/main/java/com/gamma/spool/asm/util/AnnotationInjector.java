package com.gamma.spool.asm.util;

import java.lang.annotation.Annotation;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.gamma.spool.asm.registry.AnnotationRegistry;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Utility class for injecting annotations into classes at the bytecode level.
 * <p>
 * This class provides methods to add annotations to classes using ASM bytecode manipulation.
 * It works with {@link Class} references to locate and modify the class bytecode.
 * </p>
 */
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public class AnnotationInjector {

    /**
     * Injects an annotation into a class.
     *
     * @param classNode  Class node to inject the annotation into.
     * @param annotation The annotation to inject.
     * @throws IllegalArgumentException If the annotation is not a valid annotation type.
     */
    public static boolean injectAnnotation(ClassNode classNode, AnnotationRegistry.AnnotationDescriptor annotation) {

        if (!annotation.annotationClass.isAnnotation()) {
            throw new IllegalArgumentException(annotation.annotationClass.getName() + " is not an annotation type");
        }

        // Add the annotation to the class
        return addAnnotationToClass(classNode, annotation.annotationClass, annotation.keyValuePairs);
    }

    /**
     * Adds an annotation to a ClassNode.
     *
     * @param classNode        The class node to modify.
     * @param annotationClass  The annotation class to add.
     * @param annotationValues The annotation values as key-value pairs.
     */
    private static boolean addAnnotationToClass(ClassNode classNode, Class<? extends Annotation> annotationClass,
        Object... annotationValues) {

        // Get the annotation descriptor
        String annotationDescriptor = Type.getDescriptor(annotationClass);

        // Initialize visible annotations list if needed
        if (classNode.visibleAnnotations == null) {
            classNode.visibleAnnotations = new ObjectArrayList<>();
        }

        // Check if annotation already exists
        boolean annotationExists = false;
        for (AnnotationNode existingAnnotation : classNode.visibleAnnotations) {
            if (existingAnnotation.desc.equals(annotationDescriptor)) {
                annotationExists = true;
                break;
            }
        }

        // Only add if it doesn't already exist
        if (!annotationExists) {
            AnnotationNode annotationNode = new AnnotationNode(annotationDescriptor);

            // Add annotation values if provided
            if (annotationValues != null && annotationValues.length > 0) {
                if (annotationValues.length % 2 != 0) {
                    throw new IllegalArgumentException(
                        "Annotation values must be provided as key-value pairs (even number of arguments)");
                }

                annotationNode.values = new ObjectArrayList<>();
                for (int i = 0; i < annotationValues.length; i += 2) {
                    String key = (String) annotationValues[i];
                    Object value = annotationValues[i + 1];
                    annotationNode.values.add(key);
                    annotationNode.values.add(ObjectArrayList.of(convertAnnotationValue(value)));
                }
            }

            classNode.visibleAnnotations.add(annotationNode);
            return true;
        }
        return false;
    }

    /**
     * Converts annotation values to ASM-compatible format.
     * <p>
     * This handles conversion of enum values and arrays for proper ASM representation.
     * </p>
     *
     * @param value The annotation value to convert.
     * @return The converted value suitable for ASM.
     */
    private static Object convertAnnotationValue(Object value) {
        if (value == null) {
            return null;
        }

        // Handle enum values
        if (value.getClass()
            .isEnum()) {
            Enum<?> enumValue = (Enum<?>) value;
            return new String[] { Type.getDescriptor(value.getClass()), enumValue.name() };
        }

        // Handle arrays of enums
        if (value.getClass()
            .isArray()
            && value.getClass()
                .getComponentType()
                .isEnum()) {
            Enum<?>[] enumArray = (Enum<?>[]) value;
            java.util.List<Object> convertedList = new java.util.ArrayList<>();
            for (Enum<?> enumValue : enumArray) {
                convertedList.add(new String[] { Type.getDescriptor(enumValue.getClass()), enumValue.name() });
            }
            return convertedList;
        }

        // Handle primitive arrays and object arrays
        if (value.getClass()
            .isArray()) {
            Object[] array = (Object[]) value;
            java.util.List<Object> list = new java.util.ArrayList<>();
            for (Object item : array) {
                list.add(convertAnnotationValue(item));
            }
            return list;
        }

        // Return as-is for primitive types and strings
        return value;
    }

    /**
     * Checks if a class has a specific annotation.
     *
     * @param classNode       The class node to check.
     * @param annotationClass The annotation class to look for.
     * @return true if the class has the annotation, false otherwise.
     */
    public static boolean hasAnnotation(ClassNode classNode, Class<? extends Annotation> annotationClass) {

        String annotationDescriptor = Type.getDescriptor(annotationClass);

        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) {
                if (annotation.desc.equals(annotationDescriptor)) {
                    return true;
                }
            }
        }

        return false;
    }
}
