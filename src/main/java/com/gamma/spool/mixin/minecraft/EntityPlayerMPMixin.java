package com.gamma.spool.mixin.minecraft;

import java.util.Iterator;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkWatchEvent;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

@Mixin(EntityPlayerMP.class)
public abstract class EntityPlayerMPMixin extends EntityPlayer {

    @Shadow
    @Final
    @Mutable
    public List<ChunkCoordIntPair> loadedChunks;

    public EntityPlayerMPMixin(World p_i45324_1_, GameProfile p_i45324_2_) {
        super(p_i45324_1_, p_i45324_2_);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        loadedChunks = ObjectLists.synchronize(new ObjectArrayList<>());
    }

    @Inject(method = "onUpdate()V", at = @At(value = "HEAD"), cancellable = true)
    public void onUpdate(CallbackInfo ci) {
        EntityPlayerMP instance = (EntityPlayerMP) (Object) this;

        instance.theItemInWorldManager.updateBlockRemoving();
        --instance.field_147101_bU;

        if (instance.hurtResistantTime > 0) {
            --instance.hurtResistantTime;
        }

        instance.openContainer.detectAndSendChanges();

        if (!instance.worldObj.isRemote && !ForgeHooks.canInteractWith(instance, instance.openContainer)) {
            instance.closeScreen();
            instance.openContainer = instance.inventoryContainer;
        }

        while (!instance.destroyedItemsNetCache.isEmpty()) {
            int i = Math.min(instance.destroyedItemsNetCache.size(), 127);
            int[] aint = new int[i];
            Iterator iterator = instance.destroyedItemsNetCache.iterator();
            int j = 0;

            while (iterator.hasNext() && j < i) {
                aint[j++] = ((Integer) iterator.next()).intValue();
                iterator.remove();
            }

            instance.playerNetServerHandler.sendPacket(new S13PacketDestroyEntities(aint));
        }

        if (!instance.loadedChunks.isEmpty()) {
            ObjectList<Chunk> arraylist = new ObjectArrayList<>();
            ObjectList<TileEntity> arraylist1 = new ObjectArrayList<>();
            Chunk chunk;
            synchronized (instance.loadedChunks) {
                Iterator iterator1 = instance.loadedChunks.iterator();

                while (iterator1.hasNext() && arraylist.size() < S26PacketMapChunkBulk.func_149258_c()) {
                    ChunkCoordIntPair chunkcoordintpair = (ChunkCoordIntPair) iterator1.next();

                    if (chunkcoordintpair != null) {
                        if (instance.worldObj
                            .blockExists(chunkcoordintpair.chunkXPos << 4, 0, chunkcoordintpair.chunkZPos << 4)) {
                            chunk = instance.worldObj
                                .getChunkFromChunkCoords(chunkcoordintpair.chunkXPos, chunkcoordintpair.chunkZPos);

                            if (chunk.func_150802_k()) {
                                arraylist.add(chunk);
                                arraylist1.addAll(
                                    ((WorldServer) instance.worldObj).func_147486_a(
                                        chunkcoordintpair.chunkXPos * 16,
                                        0,
                                        chunkcoordintpair.chunkZPos * 16,
                                        chunkcoordintpair.chunkXPos * 16 + 15,
                                        256,
                                        chunkcoordintpair.chunkZPos * 16 + 15));
                                // BugFix: 16 makes it load an extra chunk, which isn't associated with a player, which
                                // makes it not unload unless a player walks near it.
                                iterator1.remove();
                            }
                        }
                    } else {
                        iterator1.remove();
                    }
                }
            }

            if (!arraylist.isEmpty()) {
                instance.playerNetServerHandler.sendPacket(new S26PacketMapChunkBulk(arraylist));
                Iterator iterator2 = arraylist1.iterator();

                while (iterator2.hasNext()) {
                    TileEntity tileentity = (TileEntity) iterator2.next();
                    instance.func_147097_b(tileentity);
                }

                iterator2 = arraylist.iterator();

                while (iterator2.hasNext()) {
                    chunk = (Chunk) iterator2.next();
                    instance.getServerForPlayer()
                        .getEntityTracker()
                        .func_85172_a(instance, chunk);
                    MinecraftForge.EVENT_BUS.post(new ChunkWatchEvent.Watch(chunk.getChunkCoordIntPair(), instance));
                }
            }
        }
        ci.cancel();
    }
}
