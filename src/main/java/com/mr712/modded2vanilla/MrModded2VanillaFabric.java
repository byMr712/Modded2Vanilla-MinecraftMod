package com.mr712.modded2vanilla;

import net.fabricmc.api.ClientModInitializer;

public final class MrModded2VanillaFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        new MrModded2Vanilla().onInitializeClient();
    }
}
