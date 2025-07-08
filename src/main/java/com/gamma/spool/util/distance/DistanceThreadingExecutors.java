package com.gamma.spool.util.distance;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.gamma.spool.thread.KeyedPoolThreadManager;

public class DistanceThreadingExecutors {

    public static void execute(Chunk that, Runnable task) {
        if (lock()) {
            task.run();
            unlock();
            return;
        }
        if (!that.worldObj.getChunkProvider()
            .chunkExists(that.xPosition, that.zPosition))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task);
        else DistanceThreadingUtil.keyedPool.execute(DistanceThreadingUtil.getThread(that), task);
        unlock();
    }

    public static <A> void execute(Chunk that, Consumer<A> task, A arg1) {
        if (lock()) {
            task.accept(arg1);
            unlock();
            return;
        }
        if (!that.worldObj.getChunkProvider()
            .chunkExists(that.xPosition, that.zPosition))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task, arg1);
        else DistanceThreadingUtil.keyedPool.execute(DistanceThreadingUtil.getThread(that), task, arg1);
        unlock();
    }

    public static <A, B> void execute(Chunk that, BiConsumer<A, B> task, A arg1, B arg2) {
        if (lock()) {
            task.accept(arg1, arg2);
            unlock();
            return;
        }
        if (!that.worldObj.getChunkProvider()
            .chunkExists(that.xPosition, that.zPosition))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task, arg1, arg2);
        else DistanceThreadingUtil.keyedPool.execute(DistanceThreadingUtil.getThread(that), task, arg1, arg2);
        unlock();
    }

    public static void execute(EntityPlayer that, Runnable task) {
        if (lock()) {
            task.run();
            unlock();
            return;
        }
        if (!that.worldObj.getChunkProvider()
            .chunkExists(that.chunkCoordX, that.chunkCoordZ))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task);
        else DistanceThreadingUtil.keyedPool.execute(DistanceThreadingUtil.getThread(that), task);
        unlock();
    }

    public static <A> void execute(EntityPlayer that, Consumer<A> task, A arg1) {
        if (lock()) {
            task.accept(arg1);
            unlock();
            return;
        }
        if (!that.worldObj.getChunkProvider()
            .chunkExists(that.chunkCoordX, that.chunkCoordZ))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task, arg1);
        else DistanceThreadingUtil.keyedPool.execute(DistanceThreadingUtil.getThread(that), task, arg1);
        unlock();
    }

    public static <A, B> void execute(EntityPlayer that, BiConsumer<A, B> task, A arg1, B arg2) {
        if (lock()) {
            task.accept(arg1, arg2);
            unlock();
            return;
        }
        if (!that.worldObj.getChunkProvider()
            .chunkExists(that.chunkCoordX, that.chunkCoordZ))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task, arg1, arg2);
        else DistanceThreadingUtil.keyedPool.execute(DistanceThreadingUtil.getThread(that), task, arg1, arg2);
        unlock();
    }

    public static void execute(Entity that, Runnable task) {
        execute(that.worldObj, that.chunkCoordX, that.chunkCoordZ, task, true);
    }

    public static <A> void execute(Entity that, Consumer<A> task, A arg1) {
        execute(that.worldObj, that.chunkCoordX, that.chunkCoordZ, task, true, arg1);
    }

    public static <A, B> void execute(Entity that, BiConsumer<A, B> task, A arg1, B arg2) {
        execute(that.worldObj, that.chunkCoordX, that.chunkCoordZ, task, true, arg1, arg2);
    }

    public static void execute(TileEntity that, Runnable task) {
        execute(that.getWorldObj(), that.xCoord, that.zCoord, task, false);
    }

    public static <A> void execute(TileEntity that, Consumer<A> task, A arg1) {
        execute(that.getWorldObj(), that.xCoord, that.zCoord, task, false, arg1);
    }

    public static <A, B> void execute(TileEntity that, BiConsumer<A, B> task, A arg1, B arg2) {
        execute(that.getWorldObj(), that.xCoord, that.zCoord, task, false, arg1, arg2);
    }

    public static void execute(World worldObj, ChunkCoordIntPair that, Runnable task) {
        execute(worldObj, that.chunkXPos, that.chunkZPos, task, true);
    }

    public static <A> void execute(World worldObj, ChunkCoordIntPair that, Consumer<A> task, A arg1) {
        execute(worldObj, that.chunkXPos, that.chunkZPos, task, true, arg1);
    }

    public static <A, B> void execute(World worldObj, ChunkCoordIntPair that, BiConsumer<A, B> task, A arg1, B arg2) {
        execute(worldObj, that.chunkXPos, that.chunkZPos, task, true, arg1, arg2);
    }

    public static void execute(World worldObj, int x, int z, Runnable task, boolean chunkCoords) {
        if (lock()) {
            task.run();
            unlock();
            return;
        }
        if (!worldObj.getChunkProvider()
            .chunkExists((chunkCoords ? x : x >> 4), (chunkCoords ? z : z >> 4)))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task);
        else DistanceThreadingUtil.keyedPool.execute(
            DistanceThreadingUtil
                .getThread(worldObj.getChunkFromBlockCoords((chunkCoords ? x << 4 : x), (chunkCoords ? z << 4 : z))),
            task);
        unlock();
    }

    public static <A> void execute(World worldObj, int x, int z, Consumer<A> task, boolean chunkCoords, A arg1) {
        if (lock()) {
            task.accept(arg1);
            unlock();
            return;
        }
        if (!worldObj.getChunkProvider()
            .chunkExists((chunkCoords ? x : x >> 4), (chunkCoords ? z : z >> 4)))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task, arg1);
        else DistanceThreadingUtil.keyedPool.execute(
            DistanceThreadingUtil
                .getThread(worldObj.getChunkFromBlockCoords((chunkCoords ? x << 4 : x), (chunkCoords ? z << 4 : z))),
            task,
            arg1);
        unlock();
    }

    public static <A, B> void execute(World worldObj, int x, int z, BiConsumer<A, B> task, boolean chunkCoords, A arg1,
        B arg2) {
        if (lock()) {
            task.accept(arg1, arg2);
            unlock();
            return;
        }
        if (!worldObj.getChunkProvider()
            .chunkExists((chunkCoords ? x : x >> 4), (chunkCoords ? z : z >> 4)))
            DistanceThreadingUtil.keyedPool.execute(KeyedPoolThreadManager.MAIN_THREAD_KEY, task, arg1, arg2);
        else DistanceThreadingUtil.keyedPool.execute(
            DistanceThreadingUtil
                .getThread(worldObj.getChunkFromBlockCoords((chunkCoords ? x << 4 : x), (chunkCoords ? z << 4 : z))),
            task,
            arg1,
            arg2);
        unlock();
    }

    private static boolean lock() {
        DistanceThreadingUtil.LOCK.readLock()
            .lock();
        return !DistanceThreadingUtil.isInitialized();
    }

    private static void unlock() {
        DistanceThreadingUtil.LOCK.readLock()
            .unlock();
    }
}
