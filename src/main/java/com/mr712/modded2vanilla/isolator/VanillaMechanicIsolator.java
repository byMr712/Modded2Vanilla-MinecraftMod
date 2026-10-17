package com.mr712.modded2vanilla.isolator;

import com.mr712.modded2vanilla.state.IsolatorState;
import net.minecraft.world.item.ItemStack;

/**
 * Master Universal Vanilla Mechanic Isolator (VanillaMechanicIsolator).
 * Orchestrates and enforces strict 100% vanilla client mechanics on multiplayer servers:
 * - Movement, physics, ladder speeds, onGround invariants, and step height bounds (MovementIsolator)
 * - Block & entity interaction reach limits (InteractionIsolator)
 * - Creative stack sanitization & inventory safety (InventoryIsolator)
 *
 * In Singleplayer: all mechanics remain 100% unconstrained and functional.
 */
public final class VanillaMechanicIsolator {

    private VanillaMechanicIsolator() {
    }

    public static boolean isIsolating() {
        return IsolatorState.isIsolating();
    }

    public static ItemStack sanitizeCreativeStack(ItemStack stack) {
        return InventoryIsolator.sanitizeCreativeStack(stack);
    }
}
