package com.mr712.modded2vanilla.isolator;

import com.mr712.modded2vanilla.state.IsolatorState;
import net.minecraft.client.player.LocalPlayer;

/**
 * Universal interaction isolator for multiplayer servers.
 * Enforces vanilla block reach (4.5 survival, 5.0 creative) and entity reach (3.0),
 * preventing reach/interact anticheat kicks.
 */
public final class InteractionIsolator {

    public static final double VANILLA_SURVIVAL_BLOCK_REACH = 4.5D;
    public static final double VANILLA_CREATIVE_BLOCK_REACH = 5.0D;
    public static final double VANILLA_ENTITY_REACH = 3.0D;

    private InteractionIsolator() {
    }

    public static double sanitizeBlockInteractionRange(LocalPlayer player, double currentRange) {
        if (!IsolatorState.isIsolating()) {
            return currentRange;
        }
        double maxAllowed = (player != null && player.isCreative()) ? VANILLA_CREATIVE_BLOCK_REACH : VANILLA_SURVIVAL_BLOCK_REACH;
        if (currentRange > maxAllowed) {
            MovementIsolator.detectAndRecordCallerMod();
            return maxAllowed;
        }
        return currentRange;
    }

    public static double sanitizeEntityInteractionRange(double currentRange) {
        if (!IsolatorState.isIsolating()) {
            return currentRange;
        }
        if (currentRange > VANILLA_ENTITY_REACH) {
            MovementIsolator.detectAndRecordCallerMod();
            return VANILLA_ENTITY_REACH;
        }
        return currentRange;
    }
}
