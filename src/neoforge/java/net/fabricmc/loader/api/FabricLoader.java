package net.fabricmc.loader.api;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.metadata.ModMetadata;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface FabricLoader {
    static FabricLoader getInstance() {
        return Holder.INSTANCE;
    }

    EnvType getEnvironmentType();

    Path getConfigDir();

    default File getConfigDirectory() {
        return getConfigDir().toFile();
    }

    default Path getGameDir() {
        return Paths.get(".");
    }

    default File getGameDirectory() {
        return new File(".");
    }

    Collection<ModContainer> getAllMods();

    Optional<ModContainer> getModContainer(String id);

    default boolean isModLoaded(String id) {
        try {
            Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
            Object modList = modListClass.getMethod("get").invoke(null);
            return (boolean) modListClass.getMethod("isLoaded", String.class).invoke(modList, id);
        } catch (Throwable ignored) {
            return false;
        }
    }

    default boolean isDevelopmentEnvironment() {
        return false;
    }

    class Holder {
        private static final FabricLoader INSTANCE = new FabricLoader() {
            @Override
            public EnvType getEnvironmentType() {
                return EnvType.CLIENT;
            }

            @Override
            public Path getConfigDir() {
                return Paths.get("config");
            }

            @Override
            public Collection<ModContainer> getAllMods() {
                try {
                    Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
                    Object modList = modListClass.getMethod("get").invoke(null);
                    @SuppressWarnings("unchecked")
                    List<?> modContainers = (List<?>) modListClass.getMethod("getMods").invoke(modList);
                    if (modContainers != null) {
                        List<ModContainer> result = new ArrayList<>();
                        for (Object mc : modContainers) {
                            String modId = (String) mc.getClass().getMethod("getModId").invoke(mc);
                            String name = (String) mc.getClass().getMethod("getDisplayName").invoke(mc);
                            Object versionObj = mc.getClass().getMethod("getVersion").invoke(mc);
                            String versionStr = versionObj != null ? versionObj.toString() : "1.0";
                            Path filePath = null;
                            try {
                                Object owningFile = mc.getClass().getMethod("getOwningFile").invoke(mc);
                                Object file = owningFile.getClass().getMethod("getFile").invoke(owningFile);
                                filePath = (Path) file.getClass().getMethod("getFilePath").invoke(file);
                            } catch (Throwable ignored) {}
                            final Path finalPath = filePath;
                            result.add(new ModContainer() {
                                @Override
                                public ModMetadata getMetadata() {
                                    return new ModMetadata() {
                                        @Override
                                        public String getId() { return modId; }
                                        @Override
                                        public String getName() { return name != null ? name : modId; }
                                        @Override
                                        public Version getVersion() {
                                            return () -> versionStr;
                                        }
                                    };
                                }
                                @Override
                                public ModOrigin getOrigin() {
                                    return () -> finalPath != null ? Collections.singletonList(finalPath) : Collections.emptyList();
                                }
                                @Override
                                public Optional<Path> findPath(String path) {
                                    return Optional.empty();
                                }
                            });
                        }
                        return result;
                    }
                } catch (Throwable ignored) {}
                return Collections.emptyList();
            }

            @Override
            public Optional<ModContainer> getModContainer(String id) {
                try {
                    Class<?> modListClass = Class.forName("net.neoforged.fml.ModList");
                    Object modList = modListClass.getMethod("get").invoke(null);
                    @SuppressWarnings("unchecked")
                    Optional<?> opt = (Optional<?>) modListClass.getMethod("getModContainerById", String.class).invoke(modList, id);
                    if (opt != null && opt.isPresent()) {
                        Object container = opt.get();
                        Object modInfo = container.getClass().getMethod("getModInfo").invoke(container);
                        String name = (String) modInfo.getClass().getMethod("getDisplayName").invoke(modInfo);
                        Object versionObj = modInfo.getClass().getMethod("getVersion").invoke(modInfo);
                        String versionStr = versionObj != null ? versionObj.toString() : "1.0";
                        Path filePath = null;
                        try {
                            Object owningFile = modInfo.getClass().getMethod("getOwningFile").invoke(modInfo);
                            Object file = owningFile.getClass().getMethod("getFile").invoke(owningFile);
                            filePath = (Path) file.getClass().getMethod("getFilePath").invoke(file);
                        } catch (Throwable ignored) {}
                        final Path finalPath = filePath;
                        return Optional.of(new ModContainer() {
                            @Override
                            public ModMetadata getMetadata() {
                                return new ModMetadata() {
                                    @Override
                                    public String getId() { return id; }
                                    @Override
                                    public String getName() { return name != null ? name : id; }
                                    @Override
                                    public Version getVersion() {
                                        return () -> versionStr;
                                    }
                                };
                            }
                            @Override
                            public ModOrigin getOrigin() {
                                return () -> finalPath != null ? Collections.singletonList(finalPath) : Collections.emptyList();
                            }
                            @Override
                            public Optional<Path> findPath(String path) {
                                return Optional.empty();
                            }
                        });
                    }
                } catch (Throwable ignored) {}
                return Optional.empty();
            }
        };
    }
}
