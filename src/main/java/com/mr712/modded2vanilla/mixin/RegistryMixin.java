package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.registry.UniversalRegistryBuffer;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Registry.class)
public interface RegistryMixin {

    @Inject(
        method = "register(Lnet/minecraft/registry/Registry;Lnet/minecraft/registry/RegistryKey;Ljava/lang/Object;)Ljava/lang/Object;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static <V, T extends V> void onRegisterWithKey(Registry<V> registry, RegistryKey<V> key, T entry, CallbackInfoReturnable<T> cir) {
        if (UniversalRegistryBuffer.isBootstrappingVanilla() && key != null) {
            String namespace = key.getValue().getNamespace();
            if (!UniversalRegistryBuffer.isSystemOrVanillaNamespace(namespace)) {
                T result = UniversalRegistryBuffer.deferOrRegister(registry, key, entry);
                cir.setReturnValue(result);
            }
        }
    }

    @Inject(
        method = "register(Lnet/minecraft/registry/Registry;Lnet/minecraft/util/Identifier;Ljava/lang/Object;)Ljava/lang/Object;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static <V, T extends V> void onRegisterWithId(Registry<V> registry, Identifier id, T entry, CallbackInfoReturnable<T> cir) {
        if (UniversalRegistryBuffer.isBootstrappingVanilla() && id != null) {
            String namespace = id.getNamespace();
            if (!UniversalRegistryBuffer.isSystemOrVanillaNamespace(namespace)) {
                T result = UniversalRegistryBuffer.deferOrRegister(registry, id, entry);
                cir.setReturnValue(result);
            }
        }
    }
}
