package com.gamma.spool.compat.chunkapi;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import org.apache.logging.log4j.Level;

import com.falsepattern.chunk.internal.DataRegistryImpl;

import cpw.mods.fml.common.FMLLog;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

public class AnvilChunkLoaderCompatNonConcurrent {

    public static void writeChunkToNBT(Chunk chunk, World world, NBTTagCompound nbt) {
        nbt.setByte("V", (byte) 1);
        nbt.setInteger("xPos", chunk.xPosition);
        nbt.setInteger("zPos", chunk.zPosition);
        nbt.setLong("LastUpdate", world.getTotalWorldTime());
        nbt.setBoolean("TerrainPopulated", chunk.isTerrainPopulated);
        nbt.setLong("InhabitedTime", chunk.inhabitedTime);
        chunkapi$writeSubChunks(chunk, nbt);
        chunkapi$writeCustomData(chunk, nbt);
        chunkapi$writeEntities(chunk, world, nbt);
    }

    public static Chunk readChunkFromNBT(World world, NBTTagCompound nbt) {
        int x = nbt.getInteger("xPos");
        int z = nbt.getInteger("zPos");

        Chunk chunk = new Chunk(world, x, z);

        chunk.isTerrainPopulated = nbt.getBoolean("TerrainPopulated");
        chunk.inhabitedTime = nbt.getLong("InhabitedTime");
        chunkapi$readSubChunks(chunk, nbt);
        chunkapi$readCustomData(chunk, nbt);

        return chunk;
    }

    private static void chunkapi$readCustomData(Chunk chunk, NBTTagCompound nbt) {
        // noinspection UnstableApiUsage
        DataRegistryImpl.readChunkFromNBT(chunk, nbt);
    }

    private static void chunkapi$readSubChunks(Chunk chunk, NBTTagCompound nbt) {
        NBTTagList subChunksNBT = nbt.getTagList("Sections", 10);
        byte segments = 16;
        ExtendedBlockStorage[] subChunkList = new ExtendedBlockStorage[segments];

        for (int k = 0; k < subChunksNBT.tagCount(); ++k) {
            NBTTagCompound subChunkNBT = subChunksNBT.getCompoundTagAt(k);
            byte yLevel = subChunkNBT.getByte("Y");

            ExtendedBlockStorage subChunk = new ExtendedBlockStorage(yLevel << 4, !chunk.worldObj.provider.hasNoSky);

            // noinspection UnstableApiUsage
            DataRegistryImpl.readSubChunkFromNBT(chunk, subChunk, subChunkNBT);

            subChunk.removeInvalidBlocks();
            subChunkList[yLevel] = subChunk;
        }

        chunk.setStorageArrays(subChunkList);
    }

    private static void chunkapi$writeCustomData(Chunk chunk, NBTTagCompound nbt) {
        // noinspection UnstableApiUsage
        DataRegistryImpl.writeChunkToNBT(chunk, nbt);
    }

    private static void chunkapi$writeSubChunks(Chunk chunk, NBTTagCompound nbt) {
        ExtendedBlockStorage[] subChunks = chunk.getBlockStorageArray();
        NBTTagList subChunksNBT = new NBTTagList();
        NBTTagCompound subChunkNBT;

        for (ExtendedBlockStorage subChunk : subChunks) {
            if (subChunk != null) {
                subChunkNBT = new NBTTagCompound();
                subChunkNBT.setByte("Y", (byte) (subChunk.getYLocation() >> 4 & 255));
                // noinspection UnstableApiUsage
                DataRegistryImpl.writeSubChunkToNBT(chunk, subChunk, subChunkNBT);
                subChunksNBT.appendTag(subChunkNBT);
            }
        }

        nbt.setTag("Sections", subChunksNBT);
    }

    private static void chunkapi$writeEntities(Chunk chunk, World world, NBTTagCompound nbt) {
        chunk.hasEntities = false;
        NBTTagList entities = new NBTTagList();

        for (int i = 0; i < chunk.entityLists.length; ++i) {
            @SuppressWarnings("unchecked")
            ObjectListIterator<Entity> iterator = new ObjectArrayList<Entity>(chunk.entityLists[i]).iterator();
            while (iterator.hasNext()) {
                Entity entity = iterator.next();
                NBTTagCompound entityNBT = new NBTTagCompound();

                try {
                    if (entity.writeToNBTOptional(entityNBT)) {
                        chunk.hasEntities = true;
                        entities.appendTag(entityNBT);
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

        nbt.setTag("Entities", entities);
        NBTTagList tileEntities = new NBTTagList();

        ObjectListIterator<TileEntity> iterator = new ObjectArrayList<>(chunk.chunkTileEntityMap.values()).iterator();

        while (iterator.hasNext()) {
            TileEntity te = iterator.next();
            NBTTagCompound tileEntityNBT = new NBTTagCompound();
            try {
                te.writeToNBT(tileEntityNBT);
                tileEntities.appendTag(tileEntityNBT);
            } catch (Exception e) {
                FMLLog.log(
                    Level.ERROR,
                    e,
                    "A TileEntity type %s has throw an exception trying to write state. It will not persist. Report this to the mod author",
                    te.getClass()
                        .getName());
            }
        }

        nbt.setTag("TileEntities", tileEntities);
        List<NextTickListEntry> pendingUpdates = world.getPendingBlockUpdates(chunk, false);

        if (pendingUpdates != null) {
            long k = world.getTotalWorldTime();
            NBTTagList tileTicks = new NBTTagList();

            for (NextTickListEntry update : pendingUpdates) {
                NBTTagCompound updateNBT = new NBTTagCompound();
                updateNBT.setInteger("i", Block.getIdFromBlock(update.func_151351_a()));
                updateNBT.setInteger("x", update.xCoord);
                updateNBT.setInteger("y", update.yCoord);
                updateNBT.setInteger("z", update.zCoord);
                updateNBT.setInteger("t", (int) (update.scheduledTime - k));
                updateNBT.setInteger("p", update.priority);
                tileTicks.appendTag(updateNBT);
            }

            nbt.setTag("TileTicks", tileTicks);
        }
    }
}
