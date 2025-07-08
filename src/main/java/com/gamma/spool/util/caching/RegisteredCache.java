package com.gamma.spool.util.caching;

import java.util.Objects;

import com.gamma.spool.SpoolCoreMod;
import com.github.bsideup.jabel.Desugar;

@Desugar
public final class RegisteredCache {

    private long cachedSize = -1;
    private final ICache thisCache;

    public RegisteredCache(ICache thisCache) {
        this.thisCache = thisCache;
        this.cachedSize = thisCache.calculateSize();
    }

    public void updateCachedSize() {
        if (!SpoolCoreMod.OBJECT_DEBUG) cachedSize = -1;
        cachedSize = thisCache.calculateSize();
    }

    public long getCachedSize() {
        return cachedSize;
    }

    public ICache getCache() {
        return thisCache;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (RegisteredCache) obj;
        return this.cachedSize == that.cachedSize && Objects.equals(this.thisCache, that.thisCache);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cachedSize, thisCache);
    }

    @Override
    public String toString() {
        return "RegisteredCache[" + "cachedSize=" + cachedSize + ", " + "thisCache=" + thisCache + ']';
    }

}
