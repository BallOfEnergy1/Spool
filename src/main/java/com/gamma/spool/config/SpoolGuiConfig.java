package com.gamma.spool.config;

import net.minecraft.client.gui.GuiScreen;

import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.SimpleGuiConfig;

public class SpoolGuiConfig extends SimpleGuiConfig {

    public SpoolGuiConfig(GuiScreen parent) throws ConfigException {
        super(
            parent,
            "spool",
            "Spool",
            true,
            DebugConfig.class,
            ThreadsConfig.class,
            ThreadManagerConfig.class,
            DistanceThreadingConfig.class,
            ConcurrentConfig.class);
    }
}
