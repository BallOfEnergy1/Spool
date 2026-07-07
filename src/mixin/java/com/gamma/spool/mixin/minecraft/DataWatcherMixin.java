package com.gamma.spool.mixin.minecraft;

import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.DataWatcher;
import net.minecraft.util.ReportedException;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

// Reduce autoboxing allocations when calling `getWatchedObject()`, as well
// as improve performance by using a `StampedLock` instead of a `ReadWriteLock`.
// (No reentrancy requried).
@Mixin(DataWatcher.class)
public abstract class DataWatcherMixin {

    @Shadow
    private ReadWriteLock lock;

    @Mutable
    @Shadow
    @Final
    private Map<Integer, DataWatcher.WatchableObject> watchedObjects;

    @Unique
    private final StampedLock spool$stampedLock = new StampedLock();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        lock = spool$stampedLock.asReadWriteLock();
        watchedObjects = new Int2ObjectOpenHashMap<>();
    }

    /**
     * @author BallOfEnergy01
     * @reason Increase performance significantly.
     */
    @Overwrite
    public DataWatcher.WatchableObject getWatchedObject(int p_75684_1_) {
        long stamp = spool$stampedLock.tryOptimisticRead();
        DataWatcher.WatchableObject value;
        try {
            value = spool$getWatchedObject0(p_75684_1_);
        } finally {
            if (!spool$stampedLock.validate(stamp)) {
                stamp = spool$stampedLock.readLock();
                try {
                    value = spool$getWatchedObject0(p_75684_1_);
                } finally {
                    spool$stampedLock.unlockRead(stamp);
                }
            }
        }
        return value;
    }

    @Unique
    private DataWatcher.WatchableObject spool$getWatchedObject0(int index) {
        DataWatcher.WatchableObject watchableobject;

        try {
            watchableobject = this.watchedObjects.get(index);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Getting synched entity data");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Synched entity data");
            crashreportcategory.addCrashSection("Data ID", index);
            throw new ReportedException(crashreport);
        }

        return watchableobject;
    }
}
