package com.mr712.modded2vanilla;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(MrModded2Vanilla.MOD_ID)
public class MrModded2VanillaNeoForge {
    public MrModded2VanillaNeoForge(ModContainer modContainer) {
        MrModded2Vanilla.LOGGER.info("[MR] Modded2Vanilla (NeoForge) loaded. Modded DataTrackers will be isolated on vanilla/multiplayer servers.");
    }
}
