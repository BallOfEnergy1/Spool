package com.gamma.spool.util.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Mostly to avoid creating a lambda when returning a non-computed value in a {@link RunnableFuture}
 * 
 * @param <T> The return type.
 */
public class CompletedFuture<T> implements RunnableFuture<T> {

    public T value;

    public CompletedFuture(T endingValue) {
        value = endingValue;
    }

    @Override
    public void run() {
        // NOOP
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        // NOOP
        return false;
    }

    @Override
    public boolean isCancelled() {
        // NOOP
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return value;
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return value;
    }
}
