package com.gamma.spool;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

// die
@IFMLLoadingPlugin.Name("SpoolCoreDummy")
public class SpoolCoreModDummy implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return "";
    }

    @Override
    public String getSetupClass() {
        return "";
    }

    @Override
    public void injectData(Map<String, Object> data) {

    }

    @Override
    public String getAccessTransformerClass() {
        return "";
    }
}
