package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.isolator.MovementIsolator;
import com.mr712.modded2vanilla.state.IsolatorState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 500)
public abstract class LivingEntityMixin {

    @Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
    private void mrModded2Vanilla$clampLivingStepHeight(CallbackInfoReturnable<Float> cir) {
        if (!IsolatorState.isIsolating()) {
            return;
        }
        if ((Object) this instanceof LocalPlayer) {
            float current = cir.getReturnValue();
            if (current > MovementIsolator.VANILLA_MAX_STEP_HEIGHT) {
                cir.setReturnValue(MovementIsolator.VANILLA_MAX_STEP_HEIGHT);
                MovementIsolator.detectAndRecordCallerMod();
            }
        }
    }

    @Inject(method = "handleOnClimbable", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$pureVanillaApplyClimbingSpeed(Vec3 motion, CallbackInfoReturnable<Vec3> cir) {
        if (!IsolatorState.isIsolating()) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof LocalPlayer clientPlayer) {
            if (clientPlayer.onClimbable()) {
                clientPlayer.resetFallDistance();
                double x = Mth.clamp(motion.x, -0.15000000596046448D, 0.15000000596046448D);
                double z = Mth.clamp(motion.z, -0.15000000596046448D, 0.15000000596046448D);
                double y = Math.max(motion.y, -0.15000000596046448D);
                if (y < 0.0D && !clientPlayer.getInBlockState().is(Blocks.SCAFFOLDING) && clientPlayer.isShiftKeyDown()) {
                    y = 0.0D;
                }
                cir.setReturnValue(new Vec3(x, y, z));
            }
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$guardJumpInMultiplayer(CallbackInfo ci) {
        if (!IsolatorState.isIsolating()) {
            return;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof LocalPlayer clientPlayer) {
            // Vanilla invariant: Cannot jump mid-air without ground, ladder, or fluid contact
            if (!clientPlayer.onGround() && !clientPlayer.onClimbable() && !clientPlayer.isInWater() && !clientPlayer.isInLava()) {
                ci.cancel();
                MovementIsolator.detectAndRecordCallerMod();
            }
        }
    }
}
