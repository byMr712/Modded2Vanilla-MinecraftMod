package net.fabricmc.loader.api.metadata;

import net.fabricmc.loader.api.Version;

public interface ModMetadata {
    String getId();
    String getName();
    default Version getVersion() {
        return () -> "1.0";
    }
}
