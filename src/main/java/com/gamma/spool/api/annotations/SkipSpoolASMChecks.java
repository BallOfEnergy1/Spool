package com.gamma.spool.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation to indicate that specific ASM checks should be skipped for the annotated class.
 * This can be used to disable certain types of bytecode analysis at runtime.
 * <p>
 * The checks to be skipped can be controlled using the {@link SpoolASMCheck} enum values provided to this
 * annotation. All checks can be skipped by providing the "ALL" {@link SpoolASMCheck} value to this annotation.
 * </p>
 *
 * <p>
 * This annotation is primarily designed for use in scenarios involving bytecode manipulation
 * or runtime transformations where selected checks may interfere with expected functionality.
 * </p>
 *
 * The available {@link SpoolASMCheck} values are:
 * <ul>
 * <li>{@code ALL} - Disables all ASM checks.</li>
 * <li>{@code UNSAFE_ITERATION} - Disables checks related to unsafe iteration behaviors.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface SkipSpoolASMChecks {

    enum SpoolASMCheck {
        ALL,
        UNSAFE_ITERATION
    }

    SpoolASMCheck[] value();
}
