package com.mr712.modded2vanilla;

import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

public final class MrModded2Vanilla {
    public static final String MOD_ID = "modded2vanilla";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public void onInitializeClient() {
        LOGGER.info("MrModded2Vanilla loaded. Modded DataTrackers will be isolated on vanilla/multiplayer servers.");
        try {
            Class<?> lifecycleClass = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents");
            Object clientStartedEvent = lifecycleClass.getField("CLIENT_STARTED").get(null);
            Class<?> listenerClass = Class.forName("net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents$ClientStarted");
            Object listenerProxy = Proxy.newProxyInstance(
                listenerClass.getClassLoader(),
                new Class<?>[]{listenerClass},
                (proxy, method, args) -> {
                    AdjustmentTracker.printStartupNoticeIfAny();
                    return null;
                }
            );
            clientStartedEvent.getClass().getMethod("register", Object.class).invoke(clientStartedEvent, listenerProxy);
        } catch (Throwable ignored) {
        }
    }
}
