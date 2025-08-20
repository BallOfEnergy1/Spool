package com.gamma.spool.api;

import java.util.Optional;

import com.gamma.spool.api.statistics.IStatisticReceiver;

import cpw.mods.fml.common.event.FMLInterModComms;

/*
 * TODO: The below.
 * This API should allow other mods to:
 * 1. Toggle features inside Spool.
 * 2. View some internal statistics for Spool performance measurements.
 * 3. View the current state of Spool's initialization.
 * 4. View certain statistics about Spool's caches.
 */
@SuppressWarnings("unused")
public class SpoolAPI {

    /**
     * If Spool is initialized or not. This will always be true after the init stage.
     * <p>
     * <b>None of the API features should be used until this is true.</b>
     * Doing so can cause crashes or other unwanted artifacts.
     * </p>
     */
    public static boolean isInitialized = false;

    /**
     * If Spool's config allows other mods to gather statistics from Spool directly.
     * Mods that register statistics receivers while this is false will not receive
     * any statistics information.
     * <p>
     * This value *can* be changed while the game is running, as well as when a
     * world is open. This should be accounted for.
     * </p>
     */
    public static boolean statisticGatheringAllowed = false;

    public void registerStatisticReceiver(String modid, Class<? extends IStatisticReceiver> statisticReceiverClass) {

        modid = Optional.ofNullable(modid)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "ModID cannot be null when registering a Spool statistic receiver!"));
        statisticReceiverClass = Optional.ofNullable(statisticReceiverClass)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "Receiver class cannot be null when registering a Spool statistic receiver!"));

        if (!IStatisticReceiver.class.isAssignableFrom(statisticReceiverClass)) throw new IllegalStateException(
            "Mod " + modid
                + " attempted to register a Spool statistic receiver without the IStatisticReceiver interface.");

        FMLInterModComms.sendMessage(modid, "registerStatisticReceiver", statisticReceiverClass.getName());
    }
}
