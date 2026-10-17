package com.mr712.modded2vanilla.tracker;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AdjustmentTracker {

    private static final Set<String> AFFECTED_JARS = Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Set<String> CHECKED_CLASSES = Collections.synchronizedSet(new HashSet<>());
    private static final AtomicBoolean STARTUP_NOTICE_PRINTED = new AtomicBoolean(false);
    private static final AtomicBoolean MP_NOTICE_PRINTED = new AtomicBoolean(false);

    private AdjustmentTracker() {
    }

    public static boolean isSystemClass(String className) {
        if (className == null || className.isEmpty()) {
            return true;
        }
        return className.startsWith("net.minecraft.") ||
               className.startsWith("com.mojang.") ||
               className.startsWith("net.fabricmc.") ||
               className.startsWith("net.neoforged.") ||
               className.startsWith("net.minecraftforge.") ||
               className.startsWith("cpw.mods.") ||
               className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("jdk.") ||
               className.startsWith("sun.") ||
               className.startsWith("com.sun.") ||
               className.startsWith("org.spongepowered.") ||
               className.startsWith("org.objectweb.asm.") ||
               className.startsWith("org.slf4j.") ||
               className.startsWith("org.apache.") ||
               className.startsWith("com.google.") ||
               className.startsWith("io.netty.") ||
               className.startsWith("org.lwjgl.") ||
               className.startsWith("org.joml.") ||
               className.startsWith("org.jetbrains.") ||
               className.startsWith("it.unimi.dsi.fastutil.") ||
               className.startsWith("com.mr712.modded2vanilla.") ||
               className.startsWith("com.mr712.mrmodded2vanilla.");
    }

    public static boolean isSystemMod(String modId) {
        if (modId == null || modId.isEmpty()) {
            return true;
        }
        return "minecraft".equalsIgnoreCase(modId) ||
               "fabricloader".equalsIgnoreCase(modId) ||
               "fabric-loader".equalsIgnoreCase(modId) ||
               "fabric".equalsIgnoreCase(modId) ||
               "fabric-api".equalsIgnoreCase(modId) ||
               "neoforge".equalsIgnoreCase(modId) ||
               "forge".equalsIgnoreCase(modId) ||
               "modlauncher".equalsIgnoreCase(modId) ||
               "java".equalsIgnoreCase(modId) ||
               "brigadier".equalsIgnoreCase(modId) ||
               modId.startsWith("cpw_mods_") ||
               modId.startsWith("fabric-") ||
               modId.startsWith("fabric_") ||
               modId.startsWith("neoforge_") ||
               modId.startsWith("com_mojang_") ||
               "modded2vanilla".equalsIgnoreCase(modId) ||
               "mrmodded2vanilla".equalsIgnoreCase(modId);
    }

    public static void recordMod(String modId) {
        if (isSystemMod(modId)) {
            return;
        }
        String jarName = resolveJarName(modId);
        if (jarName.startsWith("fabric-loader") || jarName.startsWith("brigadier") || jarName.startsWith("fabric-api") || jarName.startsWith("neoforge") || jarName.startsWith("forge")) {
            return;
        }
        AFFECTED_JARS.add(jarName);
        if (IsolatorState.isMultiplayer()) {
            printNoticeIfAny();
        }
    }

    public static void recordClass(Class<?> clazz) {
        if (clazz == null) {
            return;
        }
        recordClassName(clazz.getName());
    }

    public static void recordClassName(String className) {
        if (isSystemClass(className) || !CHECKED_CLASSES.add(className)) {
            return;
        }

        // 1. Try to find mod container via loaded class CodeSource
        try {
            Class<?> clazz = Class.forName(className, false, AdjustmentTracker.class.getClassLoader());
            if (clazz.getProtectionDomain() != null && clazz.getProtectionDomain().getCodeSource() != null) {
                java.net.URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
                if (url != null) {
                    String path = url.getPath();
                    if (path.endsWith(".jar")) {
                        String fileName = java.nio.file.Paths.get(url.toURI()).getFileName().toString();
                        if (!fileName.startsWith("fabric-loader") && !fileName.startsWith("brigadier") && !fileName.startsWith("fabric-api") && !fileName.startsWith("neoforge") && !fileName.startsWith("forge")) {
                            AFFECTED_JARS.add(fileName);
                            if (IsolatorState.isMultiplayer()) {
                                printNoticeIfAny();
                            }
                            return;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        // 2. Scan all loaded mod containers by package / modId matching (Fabric or NeoForge)
        String lowerClass = className.toLowerCase();
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Iterable<?> containers = (Iterable<?>) loaderClass.getMethod("getAllMods").invoke(loader);
            for (Object container : containers) {
                Object metadata = container.getClass().getMethod("getMetadata").invoke(container);
                String modId = (String) metadata.getClass().getMethod("getId").invoke(metadata);
                if ("minecraft".equals(modId) || "fabricloader".equals(modId) || "java".equals(modId) || "neoforge".equals(modId) || "forge".equals(modId)) {
                    continue;
                }
                if (lowerClass.contains(modId.toLowerCase().replace("-", "").replace("_", ""))) {
                    recordMod(modId);
                    return;
                }
            }
            recordMod(className);
            return;
        } catch (Throwable ignored) {
        }

        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                List<?> modFiles = (List<?>) modListClass.getMethod("getModFiles").invoke(modList);
                for (Object modFileInfo : modFiles) {
                    List<?> modInfos = (List<?>) modFileInfo.getClass().getMethod("getMods").invoke(modFileInfo);
                    for (Object modInfo : modInfos) {
                        String modId = (String) modInfo.getClass().getMethod("getModId").invoke(modInfo);
                        if ("minecraft".equals(modId) || "fabricloader".equals(modId) || "java".equals(modId) || "neoforge".equals(modId) || "forge".equals(modId)) {
                            continue;
                        }
                        if (lowerClass.contains(modId.toLowerCase().replace("-", "").replace("_", ""))) {
                            recordMod(modId);
                            return;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        recordMod(className);
    }

    public static String resolveJarName(String modId) {
        try {
            Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
            Object loader = loaderClass.getMethod("getInstance").invoke(null);
            Optional<?> containerOpt = (Optional<?>) loaderClass.getMethod("getModContainer", String.class).invoke(loader, modId);
            if (containerOpt.isPresent()) {
                Object container = containerOpt.get();
                try {
                    Object origin = container.getClass().getMethod("getOrigin").invoke(container);
                    @SuppressWarnings("unchecked")
                    List<Path> paths = (List<Path>) origin.getClass().getMethod("getPaths").invoke(origin);
                    if (paths != null && !paths.isEmpty()) {
                        String fileName = paths.get(0).getFileName().toString();
                        if (fileName.endsWith(".jar")) {
                            return fileName;
                        }
                    }
                } catch (Throwable ignored) {
                }
                Object metadata = container.getClass().getMethod("getMetadata").invoke(container);
                String id = (String) metadata.getClass().getMethod("getId").invoke(metadata);
                Object version = metadata.getClass().getMethod("getVersion").invoke(metadata);
                String verStr = (String) version.getClass().getMethod("getFriendlyString").invoke(version);
                return id + "-" + verStr + ".jar";
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            if (modList != null) {
                List<?> modFiles = (List<?>) modListClass.getMethod("getModFiles").invoke(modList);
                for (Object modFileInfo : modFiles) {
                    Path filePath = null;
                    try {
                        Object fileObj = modFileInfo.getClass().getMethod("getFile").invoke(modFileInfo);
                        filePath = (Path) fileObj.getClass().getMethod("getFilePath").invoke(fileObj);
                    } catch (Throwable ignored) {
                    }
                    List<?> modInfos = (List<?>) modFileInfo.getClass().getMethod("getMods").invoke(modFileInfo);
                    for (Object modInfo : modInfos) {
                        String id = (String) modInfo.getClass().getMethod("getModId").invoke(modInfo);
                        if (modId.equals(id)) {
                            if (filePath != null) {
                                String fileName = filePath.getFileName().toString();
                                if (fileName.endsWith(".jar")) {
                                    return fileName;
                                }
                            }
                            Object versionObj = modInfo.getClass().getMethod("getVersion").invoke(modInfo);
                            return id + "-" + (versionObj != null ? versionObj.toString() : "1.0") + ".jar";
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return modId.endsWith(".jar") ? modId : (modId + ".jar");
    }

    public static void printStartupNoticeIfAny() {
        if (AFFECTED_JARS.isEmpty()) {
            return;
        }
        if (!STARTUP_NOTICE_PRINTED.compareAndSet(false, true)) {
            return;
        }
        printFormattedNotice();
    }

    public static void printNoticeIfAny() {
        if (AFFECTED_JARS.isEmpty()) {
            return;
        }
        if (!MP_NOTICE_PRINTED.compareAndSet(false, true)) {
            return;
        }
        printFormattedNotice();
    }

    private static void printFormattedNotice() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n\n");
        builder.append("==========[MrModded2Vanilla]=============\n");
        builder.append("Notice: MrModded2Vanilla detected mods incompatible with multiplayer/vanilla servers and applied runtime adjustments. \n");
        builder.append("This will not affect your singleplayer experience, enjoy your game!\n\n");
        builder.append("Affected mods: \n");
        synchronized (AFFECTED_JARS) {
            for (String jar : AFFECTED_JARS) {
                builder.append("[").append(jar).append("]\n");
            }
        }
        builder.append("==========[MrModded2Vanilla]=============\n\n");

        MrModded2Vanilla.LOGGER.info("{}", builder);
    }

    public static void resetSessionNotice() {
        MP_NOTICE_PRINTED.set(false);
    }

    public static boolean hasAdjustments() {
        return !AFFECTED_JARS.isEmpty();
    }
}
