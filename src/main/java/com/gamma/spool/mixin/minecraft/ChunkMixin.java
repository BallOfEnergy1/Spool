package com.gamma.spool.mixin.minecraft;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public abstract class ChunkMixin {

    @Shadow
    @Mutable
    private List[] entityLists;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        for (int k = 0; k < this.entityLists.length; ++k) {
            this.entityLists[k] = ObjectLists.synchronize(new ObjectArrayList<>());
        }
    }

    @Inject(method = "getEntitiesWithinAABBForEntity", at = @At("HEAD"), cancellable = true)
    public void getEntitiesWithinAABBForEntity(Entity p_76588_1_, AxisAlignedBB p_76588_2_,
        List<net.minecraft.entity.Entity> p_76588_3_, IEntitySelector p_76588_4_, CallbackInfo ci) {
        int i = MathHelper.floor_double((p_76588_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((p_76588_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
        i = MathHelper.clamp_int(i, 0, this.entityLists.length - 1);
        j = MathHelper.clamp_int(j, 0, this.entityLists.length - 1);

        for (int k = i; k <= j; ++k) {
            List list1 = this.entityLists[k];

            for (int l = 0; l < list1.size(); ++l) {
                Entity entity1 = (Entity) list1.get(l);

                if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_)
                    && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1))) {
                    p_76588_3_.add(entity1);
                    Entity[] aentity = entity1.getParts();

                    if (aentity != null) {
                        for (int i1 = 0; i1 < aentity.length; ++i1) {
                            entity1 = aentity[i1];

                            if (entity1 != p_76588_1_ && entity1.boundingBox.intersectsWith(p_76588_2_)
                                && (p_76588_4_ == null || p_76588_4_.isEntityApplicable(entity1))) {
                                p_76588_3_.add(entity1);
                            }
                        }
                    }
                }
            }
        }
        ci.cancel();
    }

    @Inject(method = "getEntitiesOfTypeWithinAAAB", at = @At("HEAD"), cancellable = true)
    public <T> void getEntitiesOfTypeWithinAAAB(Class<T> p_76618_1_, AxisAlignedBB p_76618_2_, List<T> p_76618_3_,
        IEntitySelector p_76618_4_, CallbackInfo ci) {
        int i = MathHelper.floor_double((p_76618_2_.minY - World.MAX_ENTITY_RADIUS) / 16.0D);
        int j = MathHelper.floor_double((p_76618_2_.maxY + World.MAX_ENTITY_RADIUS) / 16.0D);
        i = MathHelper.clamp_int(i, 0, this.entityLists.length - 1);
        j = MathHelper.clamp_int(j, 0, this.entityLists.length - 1);

        for (int k = i; k <= j; ++k) {
            List list1 = this.entityLists[k];

            for (int l = 0; l < list1.size(); ++l) {
                Entity entity = (Entity) list1.get(l);

                if (p_76618_1_.isAssignableFrom(entity.getClass()) && entity.boundingBox.intersectsWith(p_76618_2_)
                    && (p_76618_4_ == null || p_76618_4_.isEntityApplicable(entity))) {
                    p_76618_3_.add((T) entity);
                }
            }
        }
        ci.cancel();
    }
}
