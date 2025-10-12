package com.gamma.spool.compat.chunkapi;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;

import com.falsepattern.chunk.internal.DataRegistryImpl;
import com.gamma.spool.compat.endlessids.ConcurrentExtendedBlockStorageWrapper;
import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.concurrent.ConcurrentExtendedBlockStorage;
import com.gamma.spool.core.SpoolCompat;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ChunkCompat {

    public static void fillChunk(ConcurrentChunk that, byte[] data, int subChunkMask, int ignored,
        boolean forceUpdate) {

        Iterator<TileEntity> iterator;
        iterator = new ObjectArrayList<>(that.chunkTileEntityMap.values()).iterator();
        while (iterator.hasNext()) {
            TileEntity te = iterator.next();
            te.updateContainingBlockInfo();
            te.getBlockMetadata();
            te.getBlockType();
        }

        boolean hasSky = !that.worldObj.provider.hasNoSky;

        for (int i = 0; i < that.storageArrays.length(); i++) {
            if ((subChunkMask & (1 << i)) != 0) {
                if (that.storageArrays.get(i) == null) {
                    if (SpoolCompat.isEndlessIDsLoaded)
                        that.storageArrays.set(i, new ConcurrentExtendedBlockStorageWrapper(i << 4, hasSky));
                    else that.storageArrays.set(i, new ConcurrentExtendedBlockStorage(i << 4, hasSky));
                }
            } else if (forceUpdate && that.storageArrays.get(i) != null) {
                that.storageArrays.set(i, null);
            }
        }

        // noinspection UnstableApiUsage
        DataRegistryImpl.readFromBuffer(that, subChunkMask, forceUpdate, data);

        for (int i = 0; i < that.storageArrays.length(); ++i) {
            if ((that.storageArrays.get(i) != null) && ((subChunkMask & (1 << i)) != 0)) {
                that.storageArrays.get(i)
                    .removeInvalidBlocks();
            }
        }
        that.isLightPopulated.set(true);
        that.isTerrainPopulated.set(true);
        that.generateHeightMap();
        List<TileEntity> invalidList = new ObjectArrayList<>();

        iterator = new ObjectArrayList<>(that.chunkTileEntityMap.values()).iterator();

        while (iterator.hasNext()) {
            TileEntity te = iterator.next();
            int x = te.xCoord & 15;
            int y = te.yCoord;
            int z = te.zCoord & 15;
            Block block = te.getBlockType();
            if ((block != that.getBlock(x, y, z) || te.blockMetadata != that.getBlockMetadata(x, y, z))
                && te.shouldRefresh(
                    block,
                    that.getBlock(x, y, z),
                    te.blockMetadata,
                    that.getBlockMetadata(x, y, z),
                    that.worldObj,
                    x,
                    y,
                    z)) {
                invalidList.add(te);
            }
            te.updateContainingBlockInfo();
        }

        for (TileEntity te : invalidList) {
            te.invalidate();
        }
    }
}
