package com.gamma.spool.api.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to indicate that an abstract method should be implemented with a synchronized block
 * on a specified field.
 * <p>
 * The annotation value should be in the format:
 * {@code "Lowner/class;fieldName:Lfield/descriptor;"}
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * &#64;Synchronize(on = "Lsome/class;someField:Ljava/util/List;")
 * public abstract void targetMethod();
 * }
 *
 * </pre>
 *
 * This will be transformed to:
 *
 * <pre>
 * {@code
 * public void targetMethod() {
 *   synchronized (someClass.someField) {
 *     // original method contents
 *   }
 * }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Synchronize {

    /**
     * The field to synchronize on, in the format "Lowner/class;fieldName:Lfield/descriptor;".
     *
     * @return the field descriptor
     */
    String[] on();
}
