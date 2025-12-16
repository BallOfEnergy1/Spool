package com.gamma.spool.asm.util;

import java.util.function.Supplier;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.github.bsideup.jabel.Desugar;

@Desugar
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.ALL)
public record ParallelSupplier<T> (Supplier<T> supplier) {

    public T get() {
        synchronized (supplier) {
            return supplier.get();
        }
    }
}
