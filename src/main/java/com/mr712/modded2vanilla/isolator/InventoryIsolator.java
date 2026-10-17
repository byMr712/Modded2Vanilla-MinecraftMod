package com.mr712.modded2vanilla.isolator;

import com.mr712.modded2vanilla.component.ComponentSanitizer;
import com.mr712.modded2vanilla.state.IsolatorState;
import net.minecraft.world.item.ItemStack;

/**
 * Universal inventory isolator for multiplayer servers.
 * Strips modded components from creative stacks and protects slot interactions.
 */
public final class InventoryIsolator {

    private InventoryIsolator() {
    }

    public static ItemStack sanitizeCreativeStack(ItemStack stack) {
        if (!IsolatorState.isIsolating()) {
            return stack;
        }
        return ComponentSanitizer.sanitizeForVanillaNetwork(stack);
    }
}
