package com.gamma.spool.concurrent.providers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.ReportedException;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.chunkio.ChunkIOExecutor;

import com.gamma.gammalib.util.concurrent.IAtomic;
import com.gamma.gammalib.util.concurrent.IThreadSafe;
import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.core.SpoolCompat;
import com.gamma.spool.util.NonBlockingMCLongHashMap;
import com.mitchej123.hodgepodge.config.SpeedupsConfig;
import com.mitchej123.hodgepodge.mixins.hooks.ChunkGenScheduler;

import cpw.mods.fml.common.registry.GameRegistry;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ConcurrentChunkProviderServer extends ChunkProviderServer implements IAtomic {

    public List<Chunk> cachedChunkList;

    public final IChunkProvider currentChunkProvider;
    public final IChunkLoader currentChunkLoader;

    private final LongSet chunksToUnload = LongSets.synchronize(new LongOpenHashSet());

    public ConcurrentChunkProviderServer(WorldServer p_i1520_1_, IChunkLoader currentChunkLoader,
        IChunkProvider currentChunkProvider) {
        super(p_i1520_1_, currentChunkLoader, currentChunkProvider);
        this.currentChunkProvider = currentChunkProvider;
        this.currentChunkLoader = currentChunkLoader;
        super.loadedChunks = null;
        super.loadedChunkHashMap = new NonBlockingMCLongHashMap();
    }

    public boolean chunkExists(int x, int z) {
        long pos = ChunkCoordIntPair.chunkXZ2Int(x, z);
        Future<Chunk> future = getFuture(pos);
        return future != null && future.isDone();
    }

    public void invalidateCache() {
        cachedChunkList = null;
    }

    public List<Chunk> getChunkList() {
        if (cachedChunkList == null) cachedChunkList = ((NonBlockingMCLongHashMap) this.loadedChunkHashMap).values()
            .stream()
            .map(ConcurrentChunkProviderServer::getFutureSafe)
            .collect(ObjectArrayList.toList());
        return cachedChunkList;
    }

    public List<Chunk> func_152380_a() {
        return this.getChunkList();
    }

    /**
     * marks chunk for unload by "unload100OldestChunks" if there is no spawn point, or if the center of the chunk is
     * outside 200 blocks (x or z) of the spawn
     */
    public void unloadChunksIfNotNearSpawn(int p_73241_1_, int p_73241_2_) {
        if (this.worldObj.provider.canRespawnHere()
            && DimensionManager.shouldLoadSpawn(this.worldObj.provider.dimensionId)) {
            ChunkCoordinates chunkcoordinates = this.worldObj.getSpawnPoint();
            int k = p_73241_1_ * 16 + 8 - chunkcoordinates.posX;
            int l = p_73241_2_ * 16 + 8 - chunkcoordinates.posZ;
            short short1 = 128;

            if (k < -short1 || k > short1 || l < -short1 || l > short1) {
                this.chunksToUnload.add(ChunkCoordIntPair.chunkXZ2Int(p_73241_1_, p_73241_2_));
            }
        } else {
            this.chunksToUnload.add(ChunkCoordIntPair.chunkXZ2Int(p_73241_1_, p_73241_2_));
        }
    }

    private void unloadChunksIfNotNearSpawn(Future<Chunk> chunkFuture) {
        Chunk chunk = getFutureSafe(chunkFuture);
        unloadChunksIfNotNearSpawn(chunk.xPosition, chunk.zPosition);
    }

    /**
     * marks all chunks for unload, ignoring those near the spawn
     */
    public void unloadAllChunks() {
        for (Future<Chunk> future : ((NonBlockingMCLongHashMap) this.loadedChunkHashMap).values()) {
            this.unloadChunksIfNotNearSpawn(future);
        }
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int x, int z) {
        return loadChunk(x, z, null);
    }

    public Chunk loadChunk(int x, int z, Runnable runnable) {

        long index = ChunkCoordIntPair.chunkXZ2Int(x, z);
        Future<Chunk> future = getFuture(index);

        Chunk chunk = null;

        if (future != null) chunk = getFutureSafe(future);

        if (chunk != null && runnable != null) {
            runnable.run();
            this.chunksToUnload.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
            return chunk;
        }

        return loadChunk1(x, z, runnable);
    }

    private Chunk loadChunk1(int x, int z, Runnable runnable) {

        this.chunksToUnload.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));

        Chunk chunk;
        AnvilChunkLoader loader = null;

        if (this.currentChunkLoader instanceof AnvilChunkLoader) {
            loader = (AnvilChunkLoader) this.currentChunkLoader;
        }

        // We can only use the queue for already generated chunks
        if (loader != null && loader.chunkExists(this.worldObj, x, z)) {
            if (runnable != null) {
                ChunkIOExecutor.queueChunkLoad(this.worldObj, loader, this, x, z, runnable);
                return null;
            } else {
                chunk = ChunkIOExecutor.syncChunkLoad(this.worldObj, loader, this, x, z);
            }
            // Why. This caused so many issues when it was missing.
            long index = ChunkCoordIntPair.chunkXZ2Int(x, z);
            loadedChunkHashMap.add(index, chunk);
            invalidateCache();
        } else {
            chunk = this.originalLoadChunk1(x, z);
        }

        // If we didn't load the chunk async and have a callback run it now
        if (runnable != null) {
            runnable.run();
        }

        return chunk;
    }

    private CompletableFuture<Chunk> loadChunkCallable(int x, int z) {
        long k = ChunkCoordIntPair.chunkXZ2Int(x, z);

        final Chunk chunk1 = ForgeChunkManager.fetchDormantChunk(k, this.worldObj);
        if (chunk1 != null) return CompletableFuture.completedFuture(chunk1);

        final Chunk chunk2 = this.safeLoadChunk(x, z);
        if (chunk2 != null) return CompletableFuture.completedFuture(chunk2);

        if (this.currentChunkProvider == null) {
            return CompletableFuture.completedFuture(this.defaultEmptyChunk);
        }

        try {
            if (IThreadSafe.isConcurrent(this.currentChunkProvider))
                return CompletableFuture.completedFuture(this.currentChunkProvider.provideChunk(x, z));
            else {
                synchronized (this.currentChunkProvider) {
                    return CompletableFuture.completedFuture(this.currentChunkProvider.provideChunk(x, z));
                }
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception generating new chunk");
            CrashReportCategory crashreportcategory = crashreport.makeCategory("Chunk to be generated");
            crashreportcategory.addCrashSection("Location", String.format("%d,%d", x, z));
            crashreportcategory.addCrashSection("Position hash", k);
            crashreportcategory.addCrashSection("Generator", this.currentChunkProvider.makeString());
            throw new ReportedException(crashreport);
        }
    }

    public Chunk originalLoadChunk(int x, int z) {
        long index = ChunkCoordIntPair.chunkXZ2Int(x, z);
        Future<Chunk> future = getFuture(index);

        Chunk chunk = null;

        if (future != null) chunk = getFutureSafe(future);

        if (chunk == null) {
            return originalLoadChunk1(x, z);
        }

        this.chunksToUnload.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));

        chunk.onChunkLoad();
        populate(chunk, x, z);

        return chunk;
    }

    private Chunk originalLoadChunk1(int x, int z) {

        this.chunksToUnload.remove(ChunkCoordIntPair.chunkXZ2Int(x, z));
        long index = ChunkCoordIntPair.chunkXZ2Int(x, z);

        Future<Chunk> future = getFuture(index);

        Chunk chunk;

        if (future != null) {
            chunk = getFutureSafe(future);

            chunk.onChunkLoad();
            chunk.populateChunk(this, this, x, z);

            return chunk;
        }

        CompletableFuture<Chunk> futureTask = loadChunkCallable(x, z);

        ((NonBlockingMCLongHashMap) loadedChunkHashMap).put(index, futureTask);

        invalidateCache();

        chunk = getFutureSafe(futureTask);
        chunk.onChunkLoad();
        populate(chunk, x, z);

        return chunk;
    }

    private void populate(Chunk chunk, int x, int z) {
        if (SpoolCompat.isModLoadedFast(SpoolCompat.CompatibleMods.HODGEPODGE)
            && SpeedupsConfig.throttleChunkGeneration) {
            final var scheduler = ChunkGenScheduler.forDimension(worldObj.provider.dimensionId);
            if (scheduler.getPopulationDepth() > 0 && ChunkGenScheduler.hasTickingStarted()) {
                scheduler.deferChunkPopulation(chunk, x, z);
            } else {
                scheduler.incrementPopulationDepth();
                try {
                    chunk.populateChunk(this, this, x, z);
                } finally {
                    scheduler.decrementPopulationDepth();
                }
                scheduler.trackIfUnpopulated(chunk, x, z);
            }
        } else {
            chunk.populateChunk(this, this, x, z);
        }
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int x, int z) {
        long index = ChunkCoordIntPair.chunkXZ2Int(x, z);
        Future<Chunk> future = getFuture(index);

        Chunk chunk = null;

        if (future != null) chunk = getFutureSafe(future);

        return chunk == null
            ? (!this.worldObj.findingSpawnPoint && !this.loadChunkOnProvideRequest ? this.defaultEmptyChunk
                : this.loadChunk1(x, z, null))
            : chunk;
    }

    /**
     * used by loadChunk, but catches any exceptions if the load fails.
     */
    private Chunk safeLoadChunk(int p_73239_1_, int p_73239_2_) {
        if (this.currentChunkLoader == null) {
            return null;
        } else {
            try {
                Chunk chunk = null;
                if (this.currentChunkProvider != null) {
                    if (IThreadSafe.isConcurrent(this.currentChunkLoader))
                        chunk = this.currentChunkLoader.loadChunk(this.worldObj, p_73239_1_, p_73239_2_);
                    else {
                        synchronized (this.currentChunkLoader) {
                            chunk = this.currentChunkLoader.loadChunk(this.worldObj, p_73239_1_, p_73239_2_);
                        }
                    }
                }

                if (chunk != null) {
                    chunk.lastSaveTime = this.worldObj.getTotalWorldTime();

                    if (IThreadSafe.isConcurrent(this.currentChunkProvider))
                        this.currentChunkProvider.recreateStructures(p_73239_1_, p_73239_2_);
                    else {
                        synchronized (this.currentChunkProvider) {
                            this.currentChunkProvider.recreateStructures(p_73239_1_, p_73239_2_);
                        }
                    }
                }

                return chunk;
            } catch (Exception exception) {
                logger.error("Couldn't load chunk", exception);
                return null;
            }
        }
    }

    /**
     * used by saveChunks, but catches any exceptions if the save fails.
     */
    private void safeSaveExtraChunkData(Chunk p_73243_1_) {
        if (this.currentChunkLoader != null) {
            try {
                if (IThreadSafe.isConcurrent(this.currentChunkLoader))
                    this.currentChunkLoader.saveExtraChunkData(this.worldObj, p_73243_1_);
                else {
                    synchronized (this.currentChunkLoader) {
                        this.currentChunkLoader.saveExtraChunkData(this.worldObj, p_73243_1_);
                    }
                }
            } catch (Exception exception) {
                logger.error("Couldn't save entities", exception);
            }
        }
    }

    /**
     * used by saveChunks, but catches any exceptions if the save fails.
     */
    private void safeSaveChunk(Chunk p_73242_1_) {
        if (this.currentChunkLoader != null) {
            try {
                p_73242_1_.lastSaveTime = this.worldObj.getTotalWorldTime();
                if (IThreadSafe.isConcurrent(this.currentChunkLoader))
                    this.currentChunkLoader.saveChunk(this.worldObj, p_73242_1_);
                else {
                    synchronized (this.currentChunkLoader) {
                        this.currentChunkLoader.saveChunk(this.worldObj, p_73242_1_);
                    }
                }
            } catch (IOException ioexception) {
                logger.error("Couldn't save chunk", ioexception);
            } catch (MinecraftException minecraftexception) {
                logger
                    .error("Couldn't save chunk; already in use by another instance of Minecraft?", minecraftexception);
            }
        }
    }

    /**
     * Populates chunk with ores etc etc
     */
    public void populate(IChunkProvider p_73153_1_, int p_73153_2_, int p_73153_3_) {
        Chunk chunk = this.provideChunk(p_73153_2_, p_73153_3_);

        if (!((ConcurrentChunk) chunk).isTerrainPopulated.get()) {
            chunk.func_150809_p();

            if (this.currentChunkProvider != null) {
                if (IThreadSafe.isConcurrent(this.currentChunkProvider)) {
                    this.currentChunkProvider.populate(p_73153_1_, p_73153_2_, p_73153_3_);
                    GameRegistry.generateWorld(p_73153_2_, p_73153_3_, worldObj, currentChunkProvider, p_73153_1_);
                    chunk.setChunkModified();
                } else {
                    synchronized (this.currentChunkProvider) {
                        this.currentChunkProvider.populate(p_73153_1_, p_73153_2_, p_73153_3_);
                        GameRegistry.generateWorld(p_73153_2_, p_73153_3_, worldObj, currentChunkProvider, p_73153_1_);
                        chunk.setChunkModified();
                    }
                }
            }
        }
    }

    /**
     * Two modes of operation: if passed true, save all Chunks in one go. If passed false, save up to two chunks.
     * Return true if all chunks have been saved.
     */
    public boolean saveChunks(boolean p_73151_1_, IProgressUpdate p_73151_2_) {
        int i = 0;
        // noinspection SynchronizeOnNonFinalField
        synchronized (this.loadedChunkHashMap) {
            for (Future<Chunk> future : ((NonBlockingMCLongHashMap) this.loadedChunkHashMap).values()) {

                Chunk chunk = getFutureSafe(future);

                if (p_73151_1_) {
                    this.safeSaveExtraChunkData(chunk);
                }

                if (chunk.needsSaving(p_73151_1_)) {
                    this.safeSaveChunk(chunk);
                    ((ConcurrentChunk) chunk).isModified.set(false);
                    ++i;

                    if (i == 24 && !p_73151_1_) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Unloads chunks that are marked to be unloaded. This is not guaranteed to unload every such chunk.
     */
    public boolean unloadQueuedChunks() {
        if (!this.worldObj.levelSaving) {
            for (ChunkCoordIntPair forced : this.worldObj.getPersistentChunks()
                .keySet()) {
                this.chunksToUnload.remove(ChunkCoordIntPair.chunkXZ2Int(forced.chunkXPos, forced.chunkZPos));
            }

            LongIterator iterator = this.chunksToUnload.iterator();

            synchronized (chunksToUnload) {
                for (int i = 0; i < 100; ++i) {
                    if (!iterator.hasNext()) break;
                    long k = iterator.nextLong();

                    Future<Chunk> future = getFuture(k);

                    Chunk chunk = null;

                    if (future != null) chunk = getFutureSafe(future);

                    if (chunk != null) {
                        this.loadedChunkHashMap.remove(k);

                        boolean temp = ForgeChunkManager.getPersistentChunksFor(this.worldObj)
                            .isEmpty() && !DimensionManager.shouldLoadSpawn(this.worldObj.provider.dimensionId);
                        chunk.onChunkUnload();

                        this.safeSaveChunk(chunk);
                        this.safeSaveExtraChunkData(chunk);

                        ForgeChunkManager
                            .putDormantChunk(ChunkCoordIntPair.chunkXZ2Int(chunk.xPosition, chunk.zPosition), chunk);
                        if (getLoadedChunkCount() - 1 /* Accounting for the current unloading chunk. */ == 0 && temp) {
                            DimensionManager.unloadWorld(this.worldObj.provider.dimensionId);
                            if (IThreadSafe.isConcurrent(this.currentChunkProvider))
                                return currentChunkProvider.unloadQueuedChunks();
                            else {
                                synchronized (this.currentChunkProvider) {
                                    return currentChunkProvider.unloadQueuedChunks();
                                }
                            }
                        }
                    }
                    iterator.remove();
                }
            }

            if (this.currentChunkLoader != null) {
                if (IThreadSafe.isConcurrent(this.currentChunkLoader)) this.currentChunkLoader.chunkTick();
                else {
                    synchronized (this.currentChunkLoader) {
                        this.currentChunkLoader.chunkTick();
                    }
                }
            }
        }

        if (IThreadSafe.isConcurrent(this.currentChunkProvider)) return this.currentChunkProvider.unloadQueuedChunks();
        else {
            synchronized (this.currentChunkProvider) {
                return this.currentChunkProvider.unloadQueuedChunks();
            }
        }
    }

    private Future<Chunk> getFuture(long key) {
        return ((NonBlockingMCLongHashMap) loadedChunkHashMap).get(key);
    }

    private static Chunk getFutureSafe(Future<Chunk> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }
}
