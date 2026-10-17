package com.mr712.modded2vanilla.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mr712.modded2vanilla.render.SafeRenderHelper;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @WrapOperation(
        method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/EntityRenderState;FF)V"
        )
    )
    private void mrModded2Vanilla$wrapFeatureRender(
        FeatureRenderer<S, M> instance,
        MatrixStack matrices,
        OrderedRenderCommandQueue renderQueue,
        int light,
        net.minecraft.client.render.entity.state.EntityRenderState state,
        float limbAngle,
        float limbDistance,
        Operation<Void> original
    ) {
        if (state instanceof LivingEntityRenderState livingState) {
            SafeRenderHelper.safeRenderFeature(
                instance,
                matrices,
                renderQueue,
                light,
                (S) livingState,
                limbAngle,
                limbDistance,
                original
            );
        } else {
            original.call(instance, matrices, renderQueue, light, (S) state, limbAngle, limbDistance);
        }
    }
}
