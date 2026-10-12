package com.mr712.modded2vanilla.registry;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public final class UniversalRegistryBuffer {

    private static volatile boolean bootstrappingVanilla = false;
    private static volatile boolean flushed = false;
    private static final List<DeferredRegistration<?, ?>> PENDING_REGISTRATIONS = new ArrayList<>();

    private UniversalRegistryBuffer() {
    }

    public static void setBootstrappingVanilla(boolean bootstrapping) {
        bootstrappingVanilla = bootstrapping;
    }

    public static boolean isBootstrappingVanilla() {
        return bootstrappingVanilla && !flushed;
    }

    public static boolean isSystemOrVanillaNamespace(String namespace) {
        return "minecraft".equals(namespace) || "brigadier".equals(namespace);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <V, T extends V> T deferOrRegister(Registry<V> registry, RegistryKey<V> key, T entry) {
        if (!isBootstrappingVanilla()) {
            return Registry.register(registry, key, entry);
        }

        String namespace = key.getValue().getNamespace();
        if (isSystemOrVanillaNamespace(namespace)) {
            return Registry.register(registry, key, entry);
        }

        AdjustmentTracker.recordMod(namespace);
        PENDING_REGISTRATIONS.add(new DeferredRegistration<>(registry, key, null, entry));
        return entry;
    }

    @SuppressWarnings("unchecked")
    public static synchronized <V, T extends V> T deferOrRegister(Registry<V> registry, Identifier id, T entry) {
        if (!isBootstrappingVanilla()) {
            return Registry.register(registry, id, entry);
        }

        String namespace = id.getNamespace();
        if (isSystemOrVanillaNamespace(namespace)) {
            return Registry.register(registry, id, entry);
        }

        AdjustmentTracker.recordMod(namespace);
        PENDING_REGISTRATIONS.add(new DeferredRegistration<>(registry, null, id, entry));
        return entry;
    }

    @SuppressWarnings("unchecked")
    public static synchronized void flushAll() {
        if (flushed) {
            return;
        }
        flushed = true;
        bootstrappingVanilla = false;

        if (PENDING_REGISTRATIONS.isEmpty()) {
            return;
        }

        MrModded2Vanilla.LOGGER.info("Flushing {} deferred mod registry entries after vanilla initialization...", PENDING_REGISTRATIONS.size());

        for (final DeferredRegistration<?, ?> deferred : PENDING_REGISTRATIONS) {
            try {
                deferred.register();
            } catch (Throwable t) {
                MrModded2Vanilla.LOGGER.error("Failed to register deferred entry {}: {}", deferred, t.getMessage(), t);
            }
        }
        PENDING_REGISTRATIONS.clear();
    }

    private record DeferredRegistration<V, T extends V>(Registry<V> registry, RegistryKey<V> key, Identifier id, T entry) {
        void register() {
            if (key != null) {
                Registry.register(registry, key, entry);
            } else if (id != null) {
                Registry.register(registry, id, entry);
            }
        }
    }
}
