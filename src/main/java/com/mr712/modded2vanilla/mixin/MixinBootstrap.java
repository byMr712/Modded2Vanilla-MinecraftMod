package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.registry.UniversalRegistryBuffer;
import com.mr712.modded2vanilla.snapshot.VanillaBlockStateSnapshot;
import net.minecraft.server.Bootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public abstract class MixinBootstrap {

    @Inject(method = "bootStrap", at = @At("HEAD"))
    private static void onBootstrapStart(CallbackInfo ci) {
        UniversalRegistryBuffer.setBootstrappingVanilla(true);
    }

    @Inject(method = "bootStrap", at = @At("TAIL"))
    private static void onBootstrapInitialized(CallbackInfo ci) {
        VanillaBlockStateSnapshot.capture();
        UniversalRegistryBuffer.flushAll();
    }
}
