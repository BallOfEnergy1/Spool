package com.gamma.spool.util.concurrent.chunk;

import static com.gamma.spool.util.concurrent.chunk.BlobConstants.AREA;
import static com.gamma.spool.util.concurrent.chunk.BlobConstants.DIAMETER;
import static com.gamma.spool.util.concurrent.chunk.BlobConstants.RADIUS;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;

public class DataBlob<T> {

    private final AtomicReferenceArray<T> blob = new AtomicReferenceArray<>(AREA);
    private final int blobCenterX;
    private final int blobCenterZ;
    private final int blobMaxX;
    private final int blobMinX;
    private final int blobMaxZ;
    private final int blobMinZ;

    private final ObjectList<T> cachedList = new ObjectArrayList<>(AREA);
    private final AtomicBoolean cacheValid = new AtomicBoolean(false);
    private final AtomicInteger dataCount = new AtomicInteger(0);

    public DataBlob(int centerX, int centerZ) {
        this.blobCenterX = centerX;
        this.blobCenterZ = centerZ;
        this.blobMaxX = blobCenterX + RADIUS;
        this.blobMinX = blobCenterX - RADIUS;
        this.blobMaxZ = blobCenterZ + RADIUS;
        this.blobMinZ = blobCenterZ - RADIUS;
    }

    public boolean isCoordinateWithinBlob(int x, int z) {
        if (x - blobMinX > 0) return false;
        if (x - blobMaxX < 0) return false;
        if (z - blobMinZ > 0) return false;
        return z - blobMaxZ >= 0;
    }

    public boolean dataExistsAtCoordinates(int x, int z) {
        return blob.get(((blobMaxX - x) * DIAMETER) + (blobMaxZ - z)) != null;
    }

    public void addToBlob(int x, int z, T data) {
        blob.set(((blobMaxX - x) * DIAMETER) + (blobMaxZ - z), data);
        cacheValid.set(false);
        dataCount.getAndIncrement();
    }

    public void removeFromBlob(int x, int z) {
        clearBlobElement(((blobMaxX - x) * DIAMETER) + (blobMaxZ - z));
        cacheValid.set(false);
        dataCount.getAndDecrement();
    }

    public T getDataAtCoordinate(int x, int z) {
        return blob.get(((blobMaxX - x) * DIAMETER) + (blobMaxZ - z));
    }

    public ObjectList<T> getDataInBlob() {
        synchronized (cachedList) { // Contention will be extremely rare.
            cachedList.clear();
            if (!cacheValid.getAndSet(true)) {
                int length = blob.length();
                for (int idx = 0; idx < length; idx++) {
                    cachedList.add(idx, blob.get(idx));
                }
            }
            cachedList.removeIf(Objects::isNull);
        }
        return cachedList;
    }

    public boolean isBlobEmpty() {
        return dataCount.get() == 0;
    }

    private void clearBlobElement(int idx) {
        blob.set(idx, null);
    }
}
