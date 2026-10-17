package com.mr712.modded2vanilla.component;

import com.mr712.modded2vanilla.state.IsolatorState;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ComponentSanitizer {

    private ComponentSanitizer() {
    }

    public static ItemStack sanitizeForVanillaNetwork(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !IsolatorState.isIsolating()) {
            return stack;
        }
        if (IsolatorState.isModdedServer()) {
            return stack;
        }

        DataComponentMap components = stack.getComponents();
        if (components == null || components.isEmpty()) {
            return stack;
        }

        boolean hasModdedComponent = false;
        for (DataComponentType<?> type : components.keySet()) {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id != null && !"minecraft".equals(id.getNamespace())) {
                hasModdedComponent = true;
                AdjustmentTracker.recordMod(id.getNamespace());
                break;
            }
        }

        if (!hasModdedComponent) {
            return stack;
        }

        ItemStack copy = stack.copy();
        for (DataComponentType<?> type : components.keySet()) {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id != null && !"minecraft".equals(id.getNamespace())) {
                copy.remove(type);
            }
        }
        return copy;
    }

    public static void sanitizeInPlace(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !IsolatorState.isIsolating() || IsolatorState.isModdedServer()) {
            return;
        }

        DataComponentMap components = stack.getComponents();
        if (components == null || components.isEmpty()) {
            return;
        }

        for (DataComponentType<?> type : components.keySet()) {
            ResourceLocation id = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            if (id != null && !"minecraft".equals(id.getNamespace())) {
                stack.remove(type);
                AdjustmentTracker.recordMod(id.getNamespace());
            }
        }
    }
}
