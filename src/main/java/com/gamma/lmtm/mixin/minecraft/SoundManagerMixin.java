package com.gamma.lmtm.mixin.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraft.client.audio.SoundPoolEntry;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.common.collect.Multimap;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin {

    @Shadow
    @Final
    @Mutable
    private List tickableSounds;

    @Shadow
    private int playTime;

    @Shadow
    @Final
    private Map invPlayingSounds;

    @Shadow
    @Final
    private Map playingSounds;

    @Shadow
    @Final
    private Map playingSoundsStopTime;

    @Shadow
    @Final
    private Map playingSoundPoolEntries;

    @Shadow
    @Final
    private Map delayedSounds;

    @Shadow
    @Final
    private Multimap categorySounds;

    @Shadow
    @Final
    private static Logger logger;

    @Shadow
    @Final
    private static Marker field_148623_a;

    @Shadow
    @Final
    public SoundHandler sndHandler;

    @Shadow
    private SoundManager.SoundSystemStarterThread sndSystem;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(CallbackInfo ci) {
        tickableSounds = Collections.synchronizedList(new ArrayList<>());
    }

    @Invoker("stopSound")
    public abstract void invokeStopSound(ISound a);

    @Invoker("playSound")
    public abstract void invokePlaySound(ISound a);

    @Invoker("getNormalizedVolume")
    public abstract float invokeGetNormalizedVolume(ISound a, SoundPoolEntry b, SoundCategory c);

    @Invoker("getNormalizedPitch")
    public abstract float invokeGetNormalizedPitch(ISound a, SoundPoolEntry b);

    @Inject(method = "updateAllSounds", at = @At("INVOKE"), cancellable = true)
    public void updateAllSounds(CallbackInfo ci) {
        ++this.playTime;
        Iterator iterator = this.tickableSounds.iterator();
        String s;

        synchronized (this.tickableSounds) {
            while (iterator.hasNext()) {
                ITickableSound itickablesound = (ITickableSound) iterator.next();
                itickablesound.update();

                if (itickablesound.isDonePlaying()) {
                    this.invokeStopSound(itickablesound);
                } else {
                    s = (String) this.invPlayingSounds.get(itickablesound);
                    this.sndSystem.setVolume(
                        s,
                        this.invokeGetNormalizedVolume(
                            itickablesound,
                            (SoundPoolEntry) this.playingSoundPoolEntries.get(itickablesound),
                            this.sndHandler.getSound(itickablesound.getPositionedSoundLocation())
                                .getSoundCategory()));
                    this.sndSystem.setPitch(
                        s,
                        this.invokeGetNormalizedPitch(
                            itickablesound,
                            (SoundPoolEntry) this.playingSoundPoolEntries.get(itickablesound)));
                    this.sndSystem.setPosition(
                        s,
                        itickablesound.getXPosF(),
                        itickablesound.getYPosF(),
                        itickablesound.getZPosF());
                }
            }
        }

        iterator = this.playingSounds.entrySet()
            .iterator();
        ISound isound;

        synchronized (this.playingSounds.entrySet()) {
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                s = (String) entry.getKey();
                isound = (ISound) entry.getValue();

                if (!this.sndSystem.playing(s)) {
                    int i = ((Integer) this.playingSoundsStopTime.get(s)).intValue();

                    if (i <= this.playTime) {
                        int j = isound.getRepeatDelay();

                        if (isound.canRepeat() && j > 0) {
                            this.delayedSounds.put(isound, Integer.valueOf(this.playTime + j));
                        }

                        iterator.remove();
                        logger.debug(
                            field_148623_a,
                            "Removed channel {} because it\'s not playing anymore",
                            new Object[] { s });
                        this.sndSystem.removeSource(s);
                        this.playingSoundsStopTime.remove(s);
                        this.playingSoundPoolEntries.remove(isound);

                        try {
                            this.categorySounds.remove(
                                this.sndHandler.getSound(isound.getPositionedSoundLocation())
                                    .getSoundCategory(),
                                s);
                        } catch (RuntimeException runtimeexception) {
                            ;
                        }

                        if (isound instanceof ITickableSound) {
                            this.tickableSounds.remove(isound);
                        }
                    }
                }
            }
        }

        Iterator iterator1 = this.delayedSounds.entrySet()
            .iterator();

        synchronized (this.delayedSounds.entrySet()) {
            while (iterator1.hasNext()) {
                Map.Entry entry1 = (Map.Entry) iterator1.next();

                if (this.playTime >= ((Integer) entry1.getValue()).intValue()) {
                    isound = (ISound) entry1.getKey();

                    if (isound instanceof ITickableSound) {
                        ((ITickableSound) isound).update();
                    }

                    this.invokePlaySound(isound);
                    iterator1.remove();
                }
            }
        }
        ci.cancel();
    }
}
