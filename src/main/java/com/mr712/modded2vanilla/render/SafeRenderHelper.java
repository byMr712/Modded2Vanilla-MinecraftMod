package com.mr712.modded2vanilla.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SafeRenderHelper {

    private static final Set<String> LOGGED_CRASHING_RENDERERS = Collections.synchronizedSet(new HashSet<>());

    private SafeRenderHelper() {
    }

    public static void safeRenderFeature(
        RenderLayer<?, ?> layer,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int light,
        EntityRenderState state,
        float limbAngle,
        float limbDistance,
        Operation<Void> original
    ) {
        if (layer == null) {
            return;
        }

        if (!IsolatorState.isIsolating()) {
            original.call(layer, poseStack, submitNodeCollector, light, state, limbAngle, limbDistance);
            return;
        }

        try {
            original.call(layer, poseStack, submitNodeCollector, light, state, limbAngle, limbDistance);
        } catch (Throwable t) {
            String className = layer.getClass().getName();
            if (LOGGED_CRASHING_RENDERERS.add(className)) {
                MrModded2Vanilla.LOGGER.warn("[MrModded2Vanilla] Handled crash in RenderLayer {}: {}", className, t.getMessage());
                AdjustmentTracker.recordClassName(className);
            }
        }
    }
}
