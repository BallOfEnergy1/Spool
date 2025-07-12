package com.gamma.spool.mixin.minecraft;

import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(Chunk.class)
public abstract class ChunkMixin {

    @Shadow
    @Mutable
    public List<Entity>[] entityLists;

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        for (int k = 0; k < this.entityLists.length; ++k) {
            this.entityLists[k] = ObjectLists.synchronize(new ObjectArrayList<>());
        }
    }

    /**
     * @author BallOfEnergy01
     * @reason Concurrency.
     */
    @Overwrite
    public void getEntitiesWithinAABBForEntity(Entity p_76588_1_, AxisAlignedBB p_76588_2_, List<Entity> p_76588_3_,
        IEntitySelector p_76588_4_) {
        int i = MathHelper.floor_double((p_76588_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((p_76588_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);

        // New instance.
        List<Entity>[] newEntityLists;
        // noinspection SynchronizeOnNonFinalField
        synchronized (this.entityLists) {
            // noinspection unchecked
            newEntityLists = new ObjectArrayList[entityLists.length];
            for (int k = 0; k < entityLists.length; k++) {
                newEntityLists[k] = new ObjectArrayList<>(entityLists[k]);
            }
        }

        i = MathHelper.clamp_int(i, 0, newEntityLists.length - 1);
        j = MathHelper.clamp_int(j, 0, newEntityLists.length - 1);

        for (int k = i; k <= j; ++k) {
            List<Entity> list1 = newEntityLists[k];

            for (Entity entity1 : list1) {
                if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_)
                    && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1))) {
                    p_76588_3_.add(entity1);
                    Entity[] aentity = entity1.getParts();

                    if (aentity != null) {
                        for (Entity entity : aentity) {
                            entity1 = entity;

                            if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_)
                                && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1))) {
                                p_76588_3_.add(entity1);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @author BallOfEnergy01
     * @reason Concurrency.
     */
    @Overwrite
    public <T> void getEntitiesOfTypeWithinAAAB(Class<T> p_76618_1_, AxisAlignedBB p_76618_2_, List<T> p_76618_3_,
        IEntitySelector p_76618_4_) {
        int i = MathHelper.floor_double((p_76618_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((p_76618_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
        i = MathHelper.clamp_int(i, 0, this.entityLists.length - 1);
        j = MathHelper.clamp_int(j, 0, this.entityLists.length - 1);

        for (int k = i; k <= j; ++k) {
            List<Entity> list1 = this.entityLists[k];

            for (Entity entity : list1) {
                if (p_76618_1_.isAssignableFrom(entity.getClass()) && entity.boundingBox.intersectsWith(p_76618_2_)
                    && (p_76618_4_ == null || p_76618_4_.isEntityApplicable(entity))) {
                    // noinspection unchecked
                    p_76618_3_.add((T) entity);
                }
            }
        }
    }
}
