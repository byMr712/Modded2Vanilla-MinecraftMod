package com.mr712.modded2vanilla.state;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;

import java.util.Set;

public final class IsolatorState {
    private static volatile boolean forceEnabled = false;
    private static volatile boolean forceDisabled = false;

    private IsolatorState() {
    }

    public static boolean isIsolating() {
        if (forceDisabled) {
            return false;
        }
        if (forceEnabled) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        if (client.hasSingleplayerServer()) {
            return false;
        }
        ClientPacketListener networkHandler = client.getConnection();
        if (networkHandler == null) {
            return false;
        }
        return true;
    }

    public static boolean isCompensatingDataTracker() {
        return isIsolating();
    }

    public static boolean isSingleplayer() {
        Minecraft client = Minecraft.getInstance();
        return client != null && client.hasSingleplayerServer();
    }

    public static boolean isMultiplayer() {
        Minecraft client = Minecraft.getInstance();
        return client != null && !client.hasSingleplayerServer() && client.getConnection() != null;
    }

    public static boolean isChannelSupportedByServer(Identifier channelId) {
        if (channelId == null || "minecraft".equals(channelId.getNamespace())) {
            return true;
        }
        try {
            Class<?> cpn = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            return (boolean) cpn.getMethod("canSend", Identifier.class).invoke(null, channelId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isModdedServer() {
        if (!isMultiplayer()) {
            return false;
        }
        String brand = getServerBrand().toLowerCase();
        if (brand.contains("fabric") || brand.contains("quilt") || brand.contains("forge") || brand.contains("neoforge")) {
            return true;
        }
        try {
            Class<?> cpn = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            @SuppressWarnings("unchecked")
            Set<Identifier> sendable = (Set<Identifier>) cpn.getMethod("getSendable").invoke(null);
            if (sendable != null && !sendable.isEmpty()) {
                for (Identifier id : sendable) {
                    if (!"minecraft".equals(id.getNamespace())) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static String getServerBrand() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getConnection() == null) {
            return "unknown";
        }
        String brand = client.getConnection().serverBrand();
        return brand != null ? brand : "unknown";
    }

    public static ServerData getCurrentServerEntry() {
        Minecraft client = Minecraft.getInstance();
        return client != null ? client.getCurrentServer() : null;
    }

    public static void setForceEnabled(boolean enabled) {
        forceEnabled = enabled;
    }

    public static void setForceDisabled(boolean disabled) {
        forceDisabled = disabled;
    }
}
