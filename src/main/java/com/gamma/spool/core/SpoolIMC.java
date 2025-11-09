package com.gamma.spool.core;

import java.util.List;

import com.gamma.spool.api.statistics.IStatisticReceiver;
import com.gamma.spool.statistics.StatisticsManager;

import cpw.mods.fml.common.event.FMLInterModComms;

public class SpoolIMC {

    public static void handleIMC(FMLInterModComms.IMCEvent event) {
        for (FMLInterModComms.IMCMessage msg : event.getMessages()) {
            handleIMC(msg);
        }
    }

    public static void handleIMC(List<FMLInterModComms.IMCMessage> messages) {
        for (FMLInterModComms.IMCMessage msg : messages) {
            handleIMC(msg);
        }
    }

    public static void handleIMC(FMLInterModComms.IMCMessage message) {
        switch (message.key) {
            case ("registerStatisticReceiver"):
                registerStatisticReceiver(message.getSender(), message.getStringValue());
                break;
        }
    }

    public static void registerStatisticReceiver(String sender, String className) {
        IStatisticReceiver receiver;
        try {
            receiver = (IStatisticReceiver) Class.forName(className)
                .getConstructor()
                .newInstance();
        } catch (Throwable e) {
            SpoolLogger.error(e.toString());
            SpoolLogger.error("Failed to register statistic receiver from mod " + sender + "; Class: " + className);
            return;
        }
        SpoolLogger.info("Successfully registered statistic receiver from mod " + sender);
        StatisticsManager.registerReceiver(sender, receiver);

        SpoolLogger.info("Registered " + StatisticsManager.receiverCount() + " statistic receivers.");
    }
}
