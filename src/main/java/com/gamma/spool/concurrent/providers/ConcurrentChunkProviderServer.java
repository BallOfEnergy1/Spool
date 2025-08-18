package com.gamma.spool.concurrent.providers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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

import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.config.ConcurrentConfig;
import com.gamma.spool.util.concurrent.interfaces.IConcurrent;
import com.gamma.spool.util.concurrent.interfaces.IThreadSafe;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ConcurrentChunkProviderServer extends ChunkProviderServer implements IConcurrent {

    public final Long2ObjectMap<Chunk> concurrentLoadedChunkHashMap;
    private final boolean enableRWLock;

    public final IChunkProvider currentChunkProvider;
    public final IChunkLoader currentChunkLoader;

    private final LongSet loadingChunks = LongSets.synchronize(new LongOpenHashSet());

    private final LongSet chunksToUnload = LongSets.synchronize(new LongOpenHashSet());

    private final ReentrantReadWriteLock lock;

    @Override
    public ReentrantReadWriteLock getLock() {
        return lock;
    }

    public ConcurrentChunkProviderServer(WorldServer p_i1520_1_, IChunkLoader currentChunkLoader,
        IChunkProvider currentChunkProvider) {
        super(p_i1520_1_, currentChunkLoader, currentChunkProvider);
        this.currentChunkProvider = currentChunkProvider;
        this.currentChunkLoader = currentChunkLoader;
        super.loadedChunks = null;
        super.loadedChunkHashMap = null;
        if (ConcurrentConfig.enableRWLockChunkProvider) {
            enableRWLock = true;
            concurrentLoadedChunkHashMap = new Long2ObjectOpenHashMap<>();
            lock = new ReentrantReadWriteLock();
        } else {
            enableRWLock = false;
            concurrentLoadedChunkHashMap = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());
            lock = null;
        }
    }

    public boolean chunkExists(int p_73149_1_, int p_73149_2_) {
        long pos = ChunkCoordIntPair.chunkXZ2Int(p_73149_1_, p_73149_2_);

        if (enableRWLock) {
            readLock();
            try {
                return this.concurrentLoadedChunkHashMap.containsKey(pos);
            } finally {
                readUnlock();
            }
        } else {
            return this.concurrentLoadedChunkHashMap.containsKey(pos);
        }
    }

    // Lambda replacement.
    private static Chunk entryToChunkMapper(Long2ObjectMap.Entry<Chunk> entry) {
        return entry.getValue();
    }

    private List<Chunk> getChunkList() {
        return this.concurrentLoadedChunkHashMap.long2ObjectEntrySet()
            .stream()
            .map(ConcurrentChunkProviderServer::entryToChunkMapper)
            .collect(ObjectArrayList.toList());
    }

    public List<Chunk> func_152380_a() {
        if (enableRWLock) {
            readLock();
            try {
                return this.getChunkList();
            } finally {
                readUnlock();
            }
        } else {
            synchronized (concurrentLoadedChunkHashMap) {
                return this.getChunkList();
            }
        }
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

    /**
     * marks all chunks for unload, ignoring those near the spawn
     */
    public void unloadAllChunks() {
        if (enableRWLock) {
            readLock();
            try {
                for (Chunk chunk : this.concurrentLoadedChunkHashMap.values()) {
                    this.unloadChunksIfNotNearSpawn(chunk.xPosition, chunk.zPosition);
                }
            } finally {
                readUnlock();
            }
        } else {
            synchronized (concurrentLoadedChunkHashMap) {
                for (Chunk chunk : this.concurrentLoadedChunkHashMap.values()) {
                    this.unloadChunksIfNotNearSpawn(chunk.xPosition, chunk.zPosition);
                }
            }
        }
    }

    /**
     * loads or generates the chunk at the chunk location specified
     */
    public Chunk loadChunk(int p_73158_1_, int p_73158_2_) {
        return loadChunk(p_73158_1_, p_73158_2_, null);
    }

    @SuppressWarnings("DuplicatedCode")
    public Chunk loadChunk(int par1, int par2, Runnable runnable) {
        long k = ChunkCoordIntPair.chunkXZ2Int(par1, par2);
        Chunk chunk;
        this.chunksToUnload.remove(k);
        if (enableRWLock) {
            readLock();
            try {
                chunk = this.concurrentLoadedChunkHashMap.get(k);
            } finally {
                readUnlock();
            }
        } else {
            chunk = this.concurrentLoadedChunkHashMap.get(k);
        }

        AnvilChunkLoader loader = null;

        if (this.currentChunkLoader instanceof AnvilChunkLoader) {
            loader = (AnvilChunkLoader) this.currentChunkLoader;
        }

        // We can only use the queue for already generated chunks
        if (chunk == null && loader != null && loader.chunkExists(this.worldObj, par1, par2)) {
            if (runnable != null) {
                ChunkIOExecutor.queueChunkLoad(this.worldObj, loader, this, par1, par2, runnable);
                return null;
            } else {
                chunk = ChunkIOExecutor.syncChunkLoad(this.worldObj, loader, this, par1, par2);
            }
        } else if (chunk == null) {
            chunk = this.originalLoadChunk(par1, par2);
        }

        // If we didn't load the chunk async and have a callback run it now
        if (runnable != null) {
            runnable.run();
        }

        return chunk;
    }

    public Chunk originalLoadChunk(int p_73158_1_, int p_73158_2_) {
        Chunk chunk;
        long k = ChunkCoordIntPair.chunkXZ2Int(p_73158_1_, p_73158_2_);
        this.chunksToUnload.remove(k);

        if (enableRWLock) {
            readLock();
            try {
                chunk = this.concurrentLoadedChunkHashMap.get(k);
            } finally {
                readUnlock();
            }
        } else {
            chunk = this.concurrentLoadedChunkHashMap.get(k);
        }

        if (chunk == null) {
            boolean added = loadingChunks.add(k);
            if (!added) {
                FMLLog.bigWarning(
                    "There is an attempt to load a chunk (%d,%d) in dimension %d that is already being loaded. This will cause weird chunk breakages.",
                    p_73158_1_,
                    p_73158_2_,
                    worldObj.provider.dimensionId);
            }
            chunk = ForgeChunkManager.fetchDormantChunk(k, this.worldObj);

            if (chunk == null) {
                chunk = this.safeLoadChunk(p_73158_1_, p_73158_2_);
            }

            if (chunk == null) {
                if (this.currentChunkProvider == null) {
                    chunk = this.defaultEmptyChunk;
                } else {
                    try {
                        if (IThreadSafe.isConcurrent(this.currentChunkProvider))
                            chunk = this.currentChunkProvider.provideChunk(p_73158_1_, p_73158_2_);
                        else {
                            synchronized (this.currentChunkProvider) {
                                chunk = this.currentChunkProvider.provideChunk(p_73158_1_, p_73158_2_);
                            }
                        }
                    } catch (Throwable throwable) {
                        CrashReport crashreport = CrashReport
                            .makeCrashReport(throwable, "Exception generating new chunk");
                        CrashReportCategory crashreportcategory = crashreport.makeCategory("Chunk to be generated");
                        crashreportcategory.addCrashSection("Location", String.format("%d,%d", p_73158_1_, p_73158_2_));
                        crashreportcategory.addCrashSection("Position hash", k);
                        crashreportcategory.addCrashSection("Generator", this.currentChunkProvider.makeString());
                        throw new ReportedException(crashreport);
                    }
                }
            }

            if (enableRWLock) {
                writeLock();
                try {
                    this.concurrentLoadedChunkHashMap.put(k, chunk);
                } finally {
                    writeUnlock();
                }
            } else {
                this.concurrentLoadedChunkHashMap.put(k, chunk);
            }

            loadingChunks.remove(k);
        }

        chunk.onChunkLoad();
        chunk.populateChunk(this, this, p_73158_1_, p_73158_2_);

        return chunk;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    public Chunk provideChunk(int p_73154_1_, int p_73154_2_) {
        Chunk chunk;
        long pos = ChunkCoordIntPair.chunkXZ2Int(p_73154_1_, p_73154_2_);
        if (enableRWLock) {
            readLock();
            try {
                chunk = this.concurrentLoadedChunkHashMap.get(pos);
            } finally {
                readUnlock();
            }
        } else {
            chunk = this.concurrentLoadedChunkHashMap.get(pos);
        }

        return chunk == null
            ? (!this.worldObj.findingSpawnPoint && !this.loadChunkOnProvideRequest ? this.defaultEmptyChunk
                : this.loadChunk(p_73154_1_, p_73154_2_))
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

    private boolean saveChunks0(boolean p_73151_1_) {
        int i = 0;
        for (Chunk chunk : concurrentLoadedChunkHashMap.values()) {
            if (p_73151_1_) {
                this.safeSaveExtraChunkData(chunk);
            }

            if (chunk.needsSaving(p_73151_1_)) {
                this.safeSaveChunk(chunk);
                ((ConcurrentChunk) chunk).isModified.set(true);
                ++i;

                if (i == 24 && !p_73151_1_) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Two modes of operation: if passed true, save all Chunks in one go. If passed false, save up to two chunks.
     * Return true if all chunks have been saved.
     */
    public boolean saveChunks(boolean p_73151_1_, IProgressUpdate p_73151_2_) {
        if (enableRWLock) {
            readLock();
            try {
                return saveChunks0(p_73151_1_);
            } finally {
                readUnlock();
            }
        } else {
            synchronized (concurrentLoadedChunkHashMap) {
                return saveChunks0(p_73151_1_);
            }
        }
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
                    long chunkLong = iterator.nextLong();

                    Chunk chunk;
                    if (enableRWLock) {
                        writeLock();
                        try {
                            chunk = this.concurrentLoadedChunkHashMap.get(chunkLong);
                            this.concurrentLoadedChunkHashMap.remove(chunkLong);
                        } finally {
                            writeUnlock();
                        }
                    } else {
                        chunk = this.concurrentLoadedChunkHashMap.get(chunkLong);
                        this.concurrentLoadedChunkHashMap.remove(chunkLong);
                    }

                    if (chunk != null) {
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

    /**
     * Converts the instance data to a readable string.
     */
    public String makeString() {
        if (enableRWLock) {
            readLock();
            try {
                return "ServerChunkCache: " + this.concurrentLoadedChunkHashMap.size()
                    + " Drop: "
                    + this.chunksToUnload.size();
            } finally {
                readUnlock();
            }
        } else {
            return "ServerChunkCache: " + this.concurrentLoadedChunkHashMap.size()
                + " Drop: "
                + this.chunksToUnload.size();
        }
    }

    public int getLoadedChunkCount() {
        if (enableRWLock) {
            readLock();
            try {
                return this.concurrentLoadedChunkHashMap.size();
            } finally {
                readUnlock();
            }
        } else {
            return this.concurrentLoadedChunkHashMap.size();
        }
    }
}
