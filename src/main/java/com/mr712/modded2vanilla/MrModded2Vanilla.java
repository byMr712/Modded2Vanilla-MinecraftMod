package com.mr712.modded2vanilla;

import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MrModded2Vanilla implements ClientModInitializer {
    public static final String MOD_ID = "modded2vanilla";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("MrModded2Vanilla loaded. Modded DataTrackers will be isolated on vanilla/multiplayer servers.");
        try {
            ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
                AdjustmentTracker.printStartupNoticeIfAny();
            });
        } catch (Throwable ignored) {
        }
    }
}
