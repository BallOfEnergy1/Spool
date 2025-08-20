package com.gamma.spool.api.statistics;

public interface ICachedItem<T> {

    T getItem();

    void setItem(T item);
}
