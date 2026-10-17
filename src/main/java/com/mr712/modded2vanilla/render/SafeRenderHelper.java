package com.mr712.modded2vanilla.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.Entity;

public final class SafeRenderHelper {

    private SafeRenderHelper() {
    }

    public static void safeRenderFeature(
        RenderLayer<?, ?> layer,
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        Entity entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        Operation<Void> original
    ) {
        try {
            original.call(layer, poseStack, bufferSource, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        } catch (Throwable t) {
            String layerClass = layer != null ? layer.getClass().getName() : "UnknownLayer";
            AdjustmentTracker.recordClassName(layerClass);
            MrModded2Vanilla.LOGGER.warn("[MrModded2Vanilla] Suppressed rendering crash in layer {}: {}", layerClass, t.getMessage());
        }
    }
}
