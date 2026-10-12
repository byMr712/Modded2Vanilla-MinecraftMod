package net.neoforged.fml;

import java.util.function.Supplier;

public class ModContainer {
    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, T extension) {
    }

    public <T extends IExtensionPoint> void registerExtensionPoint(Class<T> point, Supplier<T> extension) {
    }
}
