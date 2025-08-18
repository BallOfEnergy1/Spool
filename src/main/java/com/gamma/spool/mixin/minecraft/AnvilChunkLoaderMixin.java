package com.gamma.spool.mixin.minecraft;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerArray;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import com.gamma.spool.concurrent.AtomicNibbleArray;
import com.gamma.spool.concurrent.ConcurrentChunk;
import com.gamma.spool.concurrent.ConcurrentExtendedBlockStorage;
import com.gamma.spool.config.ConcurrentConfig;

import cpw.mods.fml.common.FMLLog;

@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderMixin {

    /**
     * @author BallOfEnergy01
     * @reason Chunk concurrency.
     */
    @Overwrite
    private Chunk readChunkFromNBT(World p_75823_1_, NBTTagCompound p_75823_2_) {
        int i = p_75823_2_.getInteger("xPos");
        int j = p_75823_2_.getInteger("zPos");
        Chunk chunk;
        if (ConcurrentConfig.enableConcurrentWorldAccess) {
            chunk = new ConcurrentChunk(p_75823_1_, i, j);
            // this line is the entire reason why `heightMap` can't be final now.
            ((ConcurrentChunk) chunk).heightMap = new AtomicIntegerArray(p_75823_2_.getIntArray("HeightMap"));
        } else {
            chunk = new Chunk(p_75823_1_, i, j);
            chunk.heightMap = p_75823_2_.getIntArray("HeightMap");
        }

        if (chunk instanceof ConcurrentChunk) {
            ((ConcurrentChunk) chunk).isTerrainPopulated.set(p_75823_2_.getBoolean("TerrainPopulated"));
            ((ConcurrentChunk) chunk).isLightPopulated.set(p_75823_2_.getBoolean("LightPopulated"));
        } else {
            chunk.isTerrainPopulated = p_75823_2_.getBoolean("TerrainPopulated");
            chunk.isLightPopulated = p_75823_2_.getBoolean("LightPopulated");
        }

        chunk.inhabitedTime = p_75823_2_.getLong("InhabitedTime");
        NBTTagList nbttaglist = p_75823_2_.getTagList("Sections", 10);
        ExtendedBlockStorage[] aextendedblockstorage;
        if (ConcurrentConfig.enableConcurrentWorldAccess)
            aextendedblockstorage = new ConcurrentExtendedBlockStorage[16];
        else aextendedblockstorage = new ExtendedBlockStorage[16];
        boolean flag = !p_75823_1_.provider.hasNoSky;

        for (int k = 0; k < nbttaglist.tagCount(); ++k) {
            NBTTagCompound nbttagcompound1 = nbttaglist.getCompoundTagAt(k);
            byte b1 = nbttagcompound1.getByte("Y");
            ExtendedBlockStorage extendedblockstorage;

            if (ConcurrentConfig.enableConcurrentWorldAccess)
                extendedblockstorage = new ConcurrentExtendedBlockStorage(b1 << 4, flag);
            else extendedblockstorage = new ExtendedBlockStorage(b1 << 4, flag);

            extendedblockstorage.setBlockLSBArray(nbttagcompound1.getByteArray("Blocks"));

            if (nbttagcompound1.hasKey("Add", 7)) {
                if (ConcurrentConfig.enableConcurrentWorldAccess) {
                    extendedblockstorage
                        .setBlockMSBArray(new AtomicNibbleArray(nbttagcompound1.getByteArray("Add"), 4));
                } else {
                    extendedblockstorage.setBlockMSBArray(new NibbleArray(nbttagcompound1.getByteArray("Add"), 4));
                }
            }

            if (ConcurrentConfig.enableConcurrentWorldAccess) {
                extendedblockstorage
                    .setBlockMetadataArray(new AtomicNibbleArray(nbttagcompound1.getByteArray("Data"), 4));
                extendedblockstorage
                    .setBlocklightArray(new AtomicNibbleArray(nbttagcompound1.getByteArray("BlockLight"), 4));
                if (flag) {
                    extendedblockstorage
                        .setSkylightArray(new AtomicNibbleArray(nbttagcompound1.getByteArray("SkyLight"), 4));
                }
            } else {
                extendedblockstorage.setBlockMetadataArray(new NibbleArray(nbttagcompound1.getByteArray("Data"), 4));
                extendedblockstorage.setBlocklightArray(new NibbleArray(nbttagcompound1.getByteArray("BlockLight"), 4));
                if (flag) {
                    extendedblockstorage.setSkylightArray(new NibbleArray(nbttagcompound1.getByteArray("SkyLight"), 4));
                }
            }

            extendedblockstorage.removeInvalidBlocks();
            aextendedblockstorage[b1] = extendedblockstorage;
        }

        chunk.setStorageArrays(aextendedblockstorage);

        if (p_75823_2_.hasKey("Biomes", 7)) {
            chunk.setBiomeArray(p_75823_2_.getByteArray("Biomes"));
        }

        // End this method here and split off entity loading to another method
        return chunk;
    }

    /**
     * @author BallOfEnergy01
     * @reason Chunk concurrency.
     */
    @Overwrite
    private void writeChunkToNBT(Chunk p_75820_1_, World p_75820_2_, NBTTagCompound p_75820_3_) {
        p_75820_3_.setByte("V", (byte) 1);
        if (ConcurrentConfig.enableConcurrentWorldAccess) ((ConcurrentChunk) p_75820_1_).readLock();
        p_75820_3_.setInteger("xPos", p_75820_1_.xPosition);
        p_75820_3_.setInteger("zPos", p_75820_1_.zPosition);
        p_75820_3_.setLong("LastUpdate", p_75820_2_.getTotalWorldTime());
        if (ConcurrentConfig.enableConcurrentWorldAccess) {
            AtomicIntegerArray heightMapArray = ((ConcurrentChunk) p_75820_1_).heightMap;
            int[] outputHeightMap = new int[heightMapArray.length()];
            for (int i = 0; i < heightMapArray.length(); i++) {
                outputHeightMap[i] = heightMapArray.get(i);
            }
            p_75820_3_.setIntArray("HeightMap", outputHeightMap);
            p_75820_3_.setBoolean("TerrainPopulated", ((ConcurrentChunk) p_75820_1_).isTerrainPopulated.get());
            p_75820_3_.setBoolean("LightPopulated", ((ConcurrentChunk) p_75820_1_).isLightPopulated.get());
        } else {
            p_75820_3_.setIntArray("HeightMap", p_75820_1_.heightMap);
            p_75820_3_.setBoolean("TerrainPopulated", p_75820_1_.isTerrainPopulated);
            p_75820_3_.setBoolean("LightPopulated", p_75820_1_.isLightPopulated);
        }

        p_75820_3_.setLong("InhabitedTime", p_75820_1_.inhabitedTime);
        ExtendedBlockStorage[] aextendedblockstorage = p_75820_1_.getBlockStorageArray();
        NBTTagList nbttaglist = new NBTTagList();
        boolean flag = !p_75820_2_.provider.hasNoSky;
        int i = aextendedblockstorage.length;
        NBTTagCompound nbttagcompound1;

        for (int j = 0; j < i; ++j) {
            ExtendedBlockStorage extendedblockstorage = aextendedblockstorage[j];

            if (extendedblockstorage != null) {
                nbttagcompound1 = new NBTTagCompound();
                nbttagcompound1.setByte("Y", (byte) (extendedblockstorage.getYLocation() >> 4 & 255));
                nbttagcompound1.setByteArray("Blocks", extendedblockstorage.getBlockLSBArray());

                if (extendedblockstorage.getBlockMSBArray() != null) {
                    if (ConcurrentConfig.enableConcurrentWorldAccess) nbttagcompound1.setByteArray(
                        "Add",
                        ((AtomicNibbleArray) extendedblockstorage.getBlockMSBArray()).getByteArray());
                    else nbttagcompound1.setByteArray("Add", extendedblockstorage.getBlockMSBArray().data);
                }

                if (ConcurrentConfig.enableConcurrentWorldAccess) nbttagcompound1
                    .setByteArray("Data", ((AtomicNibbleArray) extendedblockstorage.getMetadataArray()).getByteArray());
                else nbttagcompound1.setByteArray("Data", extendedblockstorage.getMetadataArray().data);

                if (ConcurrentConfig.enableConcurrentWorldAccess) nbttagcompound1.setByteArray(
                    "BlockLight",
                    ((AtomicNibbleArray) extendedblockstorage.getBlocklightArray()).getByteArray());
                else nbttagcompound1.setByteArray("BlockLight", extendedblockstorage.getBlocklightArray().data);

                if (flag) {
                    if (ConcurrentConfig.enableConcurrentWorldAccess) nbttagcompound1.setByteArray(
                        "SkyLight",
                        ((AtomicNibbleArray) extendedblockstorage.getSkylightArray()).getByteArray());
                    else nbttagcompound1.setByteArray("SkyLight", extendedblockstorage.getSkylightArray().data);
                } else {
                    if (ConcurrentConfig.enableConcurrentWorldAccess) nbttagcompound1.setByteArray(
                        "SkyLight",
                        new byte[((AtomicNibbleArray) extendedblockstorage.getBlocklightArray())
                            .getByteArray().length]);
                    else nbttagcompound1
                        .setByteArray("SkyLight", new byte[extendedblockstorage.getBlocklightArray().data.length]);
                }

                nbttaglist.appendTag(nbttagcompound1);
            }
        }

        p_75820_3_.setTag("Sections", nbttaglist);
        p_75820_3_.setByteArray("Biomes", p_75820_1_.getBiomeArray());
        if (ConcurrentConfig.enableConcurrentWorldAccess) {
            ((ConcurrentChunk) p_75820_1_).hasEntities.set(false);
        } else {
            p_75820_1_.hasEntities = false;
        }
        NBTTagList nbttaglist2 = new NBTTagList();
        @SuppressWarnings("rawtypes")
        Iterator iterator1;

        if (ConcurrentConfig.enableConcurrentWorldAccess) {
            for (i = 0; i < p_75820_1_.entityLists.length; ++i) {
                iterator1 = p_75820_1_.entityLists[i].iterator();

                while (iterator1.hasNext()) {
                    Entity entity = (Entity) iterator1.next();
                    nbttagcompound1 = new NBTTagCompound();

                    try {
                        if (entity.writeToNBTOptional(nbttagcompound1)) {
                            if (ConcurrentConfig.enableConcurrentWorldAccess)
                                ((ConcurrentChunk) p_75820_1_).hasEntities.set(true);
                            else p_75820_1_.hasEntities = true;
                            nbttaglist2.appendTag(nbttagcompound1);
                        }
                    } catch (Exception e) {
                        FMLLog.log(
                            Level.ERROR,
                            e,
                            "An Entity type %s has thrown an exception trying to write state. It will not persist. Report this to the mod author",
                            entity.getClass()
                                .getName());
                    }
                }
            }
        } else {
            // noinspection SynchronizeOnNonFinalField
            synchronized (p_75820_1_.entityLists) {
                for (i = 0; i < p_75820_1_.entityLists.length; ++i) {
                    iterator1 = p_75820_1_.entityLists[i].iterator();

                    while (iterator1.hasNext()) {
                        Entity entity = (Entity) iterator1.next();
                        nbttagcompound1 = new NBTTagCompound();

                        try {
                            if (entity.writeToNBTOptional(nbttagcompound1)) {
                                if (ConcurrentConfig.enableConcurrentWorldAccess)
                                    ((ConcurrentChunk) p_75820_1_).hasEntities.set(true);
                                else p_75820_1_.hasEntities = true;
                                nbttaglist2.appendTag(nbttagcompound1);
                            }
                        } catch (Exception e) {
                            FMLLog.log(
                                Level.ERROR,
                                e,
                                "An Entity type %s has thrown an exception trying to write state. It will not persist. Report this to the mod author",
                                entity.getClass()
                                    .getName());
                        }
                    }
                }
            }
        }

        p_75820_3_.setTag("Entities", nbttaglist2);
        NBTTagList nbttaglist3 = new NBTTagList();
        iterator1 = p_75820_1_.chunkTileEntityMap.values()
            .iterator();

        //noinspection SynchronizeOnNonFinalField
        synchronized (p_75820_1_.chunkTileEntityMap) {
            while (iterator1.hasNext()) {
                TileEntity tileentity = (TileEntity) iterator1.next();
                nbttagcompound1 = new NBTTagCompound();
                try {
                    tileentity.writeToNBT(nbttagcompound1);
                    nbttaglist3.appendTag(nbttagcompound1);
                } catch (Exception e) {
                    FMLLog.log(
                        Level.ERROR,
                        e,
                        "A TileEntity type %s has throw an exception trying to write state. It will not persist. Report this to the mod author",
                        tileentity.getClass()
                            .getName());
                }
            }
        }

        p_75820_3_.setTag("TileEntities", nbttaglist3);
        List<NextTickListEntry> list = p_75820_2_.getPendingBlockUpdates(p_75820_1_, false);

        if (ConcurrentConfig.enableConcurrentWorldAccess) ((ConcurrentChunk) p_75820_1_).readUnlock();

        if (list != null) {
            long k = p_75820_2_.getTotalWorldTime();
            NBTTagList nbttaglist1 = new NBTTagList();

            for (NextTickListEntry nextticklistentry : list) {
                NBTTagCompound nbttagcompound2 = new NBTTagCompound();
                nbttagcompound2.setInteger("i", Block.getIdFromBlock(nextticklistentry.func_151351_a()));
                nbttagcompound2.setInteger("x", nextticklistentry.xCoord);
                nbttagcompound2.setInteger("y", nextticklistentry.yCoord);
                nbttagcompound2.setInteger("z", nextticklistentry.zCoord);
                nbttagcompound2.setInteger("t", (int) (nextticklistentry.scheduledTime - k));
                nbttagcompound2.setInteger("p", nextticklistentry.priority);
                nbttaglist1.appendTag(nbttagcompound2);
            }

            p_75820_3_.setTag("TileTicks", nbttaglist1);
        }
    }
}
