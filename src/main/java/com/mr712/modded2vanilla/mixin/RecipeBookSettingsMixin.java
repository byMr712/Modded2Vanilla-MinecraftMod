package com.mr712.modded2vanilla.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.stats.RecipeBookSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.Constructor;
import java.util.Map;

@Mixin(RecipeBookSettings.class)
public abstract class RecipeBookSettingsMixin {

    private static final Constructor<?> TYPE_SETTINGS_CTOR;

    static {
        Constructor<?> ctor = null;
        try {
            Class<?> clazz = Class.forName("net.minecraft.stats.RecipeBookSettings$TypeSettings");
            ctor = clazz.getDeclaredConstructor(boolean.class, boolean.class);
            ctor.setAccessible(true);
        } catch (Throwable ignored) {
        }
        TYPE_SETTINGS_CTOR = ctor;
    }

    private static Object createDefaultTypeSettings() {
        if (TYPE_SETTINGS_CTOR != null) {
            try {
                return TYPE_SETTINGS_CTOR.newInstance(false, false);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    @WrapOperation(
        method = {"replaceFrom", "copy"},
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private Object mrModded2Vanilla$fallbackNullTypeSettings(Map<?, ?> map, Object key, Operation<Object> original) {
        Object result = original.call(map, key);
        if (result == null) {
            return createDefaultTypeSettings();
        }
        return result;
    }
}
