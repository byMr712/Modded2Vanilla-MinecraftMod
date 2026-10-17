package com.mr712.modded2vanilla.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SafeRenderHelper {

    private static final Set<String> LOGGED_CRASHING_RENDERERS = Collections.synchronizedSet(new HashSet<>());

    private SafeRenderHelper() {
    }

    public static <S extends LivingEntityRenderState> void safeRenderFeature(
        FeatureRenderer<S, ?> featureRenderer,
        MatrixStack matrices,
        OrderedRenderCommandQueue renderQueue,
        int light,
        S state,
        float limbAngle,
        float limbDistance,
        Operation<Void> original
    ) {
        if (featureRenderer == null) {
            return;
        }

        if (!IsolatorState.isIsolating()) {
            original.call(featureRenderer, matrices, renderQueue, light, state, limbAngle, limbDistance);
            return;
        }

        try {
            original.call(featureRenderer, matrices, renderQueue, light, state, limbAngle, limbDistance);
        } catch (Throwable t) {
            String className = featureRenderer.getClass().getName();
            if (LOGGED_CRASHING_RENDERERS.add(className)) {
                MrModded2Vanilla.LOGGER.warn("[MrModded2Vanilla] Handled crash in FeatureRenderer {}: {}", className, t.getMessage());
                AdjustmentTracker.recordClassName(className);
            }
        }
    }
}
