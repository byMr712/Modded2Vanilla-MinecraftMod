package com.mr712.modded2vanilla.state;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.util.Identifier;

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false;
        }
        // In singleplayer, all mods and modded blocks remain completely functional
        if (client.isInSingleplayer()) {
            return false;
        }
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler == null) {
            return false;
        }
        // In multiplayer, isolate vanilla registries and entity metadata from mod pollution
        return true;
    }

    public static boolean isCompensatingDataTracker() {
        return isIsolating();
    }

    public static boolean isSingleplayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.isInSingleplayer();
    }

    public static boolean isMultiplayer() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && !client.isInSingleplayer();
    }

    public static boolean isChannelSupportedByServer(Identifier channelId) {
        if (channelId == null) {
            return true;
        }
        String namespace = channelId.getNamespace();
        String path = channelId.getPath();

        // Never send channel registration packets (minecraft:register / unregister) to non-modded servers (avoids Waterfall/BungeeCord kicks)
        if ("minecraft".equals(namespace) && ("register".equals(path) || "unregister".equals(path))) {
            return isModdedServer();
        }

        if ("minecraft".equals(namespace)) {
            return true;
        }
        if (isModdedServer()) {
            return true;
        }
        try {
            Class<?> ccn = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking");
            if ((boolean) ccn.getMethod("canSend", channelId.getClass()).invoke(null, channelId)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> cpn = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            if ((boolean) cpn.getMethod("canSend", channelId.getClass()).invoke(null, channelId)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isModdedNamespace(String namespace) {
        if (namespace == null) {
            return false;
        }
        String ns = namespace.toLowerCase();
        return ns.equals("fabric") || ns.startsWith("fabric-")
            || ns.equals("quilt") || ns.startsWith("quilt-")
            || ns.equals("forge") || ns.startsWith("forge-")
            || ns.equals("neoforge") || ns.startsWith("neoforge-");
    }

    private static boolean hasModdedLoaderChannel() {
        try {
            Class<?> cpn = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking");
            @SuppressWarnings("unchecked")
            Set<?> sendable = (Set<?>) cpn.getMethod("getSendable").invoke(null);
            if (sendable != null && !sendable.isEmpty()) {
                for (Object idObj : sendable) {
                    if (idObj != null && isModdedNamespace(idObj.toString().split(":")[0])) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> ccn = Class.forName("net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking");
            @SuppressWarnings("unchecked")
            Set<?> sendableConfig = (Set<?>) ccn.getMethod("getSendable").invoke(null);
            if (sendableConfig != null && !sendableConfig.isEmpty()) {
                for (Object idObj : sendableConfig) {
                    if (idObj != null && isModdedNamespace(idObj.toString().split(":")[0])) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isModdedServer() {
        if (!isMultiplayer()) {
            return false;
        }
        String brand = getServerBrand().toLowerCase();
        if (brand.contains("fabric") || brand.contains("quilt") || brand.contains("forge") || brand.contains("neoforge")) {
            return true;
        }
        return hasModdedLoaderChannel();
    }

    public static String getServerBrand() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getNetworkHandler() == null) {
            return "unknown";
        }
        String brand = client.getNetworkHandler().getBrand();
        return brand != null ? brand : "unknown";
    }

    public static ServerInfo getCurrentServerEntry() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null ? client.getCurrentServerEntry() : null;
    }

    public static void setForceEnabled(boolean enabled) {
        forceEnabled = enabled;
    }

    public static void setForceDisabled(boolean disabled) {
        forceDisabled = disabled;
    }
}

