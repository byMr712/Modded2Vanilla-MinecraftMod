package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.isolator.VanillaMechanicIsolator;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MultiPlayerGameMode.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @ModifyVariable(
        method = "handleCreativeModeItemAdd",
        at = @At("HEAD"),
        argsOnly = true
    )
    private ItemStack mrModded2Vanilla$sanitizeCreativeStack(ItemStack stack) {
        return VanillaMechanicIsolator.sanitizeCreativeStack(stack);
    }
}
