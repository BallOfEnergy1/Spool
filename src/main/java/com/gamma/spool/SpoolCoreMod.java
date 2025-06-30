package com.gamma.spool;

import java.util.Map;

import com.gamma.spool.config.DebugConfig;
import com.gamma.spool.config.ThreadManagerConfig;
import com.gamma.spool.config.ThreadsConfig;
import com.gtnewhorizon.gtnhlib.config.ConfigException;
import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

// die
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class SpoolCoreMod implements IFMLLoadingPlugin {

    static {
        try {
            ConfigurationManager.registerConfig(DebugConfig.class);
            ConfigurationManager.registerConfig(ThreadsConfig.class);
            ConfigurationManager.registerConfig(ThreadManagerConfig.class);
        } catch (ConfigException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
