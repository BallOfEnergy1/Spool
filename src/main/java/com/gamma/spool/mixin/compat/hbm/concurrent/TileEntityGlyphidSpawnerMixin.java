package com.gamma.spool.mixin.compat.hbm.concurrent;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumDifficulty;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.gamma.spool.api.annotations.SkipSpoolASMChecks;
import com.hbm.blocks.generic.BlockGlyphidSpawner;
import com.hbm.config.MobConfig;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import com.hbm.entity.mob.glyphid.EntityGlyphidScout;
import com.hbm.handler.pollution.PollutionHandler;

@Mixin(value = BlockGlyphidSpawner.TileEntityGlpyhidSpawner.class, remap = false)
@SkipSpoolASMChecks(SkipSpoolASMChecks.SpoolASMCheck.UNSAFE_ITERATION)
public abstract class TileEntityGlyphidSpawnerMixin extends TileEntity {

    @Shadow(remap = false)
    boolean initialSpawn;

    @Shadow(remap = false)
    public abstract ArrayList<EntityGlyphid> createSwarm(float soot, int meta);

    @Shadow(remap = false)
    public abstract void trySpawnEntity(EntityGlyphid glyphid);

    /**
     * @author BallOfEnergy01
     * @reason Manually fix concurrency.
     */
    @Overwrite(remap = false)
    public void updateEntity() {
        if (!this.worldObj.isRemote && this.worldObj.difficultySetting != EnumDifficulty.PEACEFUL
            && (this.initialSpawn || this.worldObj.getTotalWorldTime() % (long) MobConfig.swarmCooldown == 0L)) {
            this.initialSpawn = false;
            int count = 0;

            // noinspection SynchronizeOnNonFinalField
            synchronized (this.worldObj.loadedEntityList) {
                for (Entity e : this.worldObj.loadedEntityList) {
                    if (e instanceof EntityGlyphid) {
                        ++count;
                        if ((double) count >= MobConfig.spawnMax) {
                            return;
                        }
                    }
                }
            }

            List<EntityGlyphid> list = this.worldObj.getEntitiesWithinAABB(
                EntityGlyphid.class,
                AxisAlignedBB.getBoundingBox(
                    this.xCoord - 5,
                    this.yCoord + 1,
                    this.zCoord - 5,
                    this.xCoord + 6,
                    this.yCoord + 7,
                    this.zCoord + 6));
            float soot = PollutionHandler.getPollution(
                this.worldObj,
                this.xCoord,
                this.yCoord,
                this.zCoord,
                PollutionHandler.PollutionType.SOOT);
            int subtype = this.getBlockMetadata();
            if (list.size() <= 3 || subtype == 2) {
                for (EntityGlyphid glyphid : this.createSwarm(soot, subtype)) {
                    this.trySpawnEntity(glyphid);
                }

                if (!this.initialSpawn && this.worldObj.rand.nextInt(MobConfig.scoutSwarmSpawnChance + 1) == 0
                    && (double) soot >= MobConfig.scoutThreshold
                    && subtype != 2) {
                    EntityGlyphidScout scout = new EntityGlyphidScout(this.worldObj);
                    if (this.getBlockMetadata() == 1) {
                        scout.getDataWatcher()
                            .updateObject(18, (byte) 1);
                    }

                    this.trySpawnEntity(scout);
                }
            }
        }
    }
}
