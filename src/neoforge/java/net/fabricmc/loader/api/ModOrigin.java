package net.fabricmc.loader.api;

import java.nio.file.Path;
import java.util.List;

public interface ModOrigin {
    List<Path> getPaths();
}
