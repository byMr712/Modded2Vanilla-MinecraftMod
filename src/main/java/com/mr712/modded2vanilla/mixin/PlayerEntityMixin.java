package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.isolator.InteractionIsolator;
import com.mr712.modded2vanilla.isolator.MovementIsolator;
import com.mr712.modded2vanilla.state.IsolatorState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, priority = 2000)
public abstract class PlayerEntityMixin {

    @Inject(method = "isStayingOnGroundSurface", at = @At("RETURN"), cancellable = true)
    private void mrModded2Vanilla$guardClipAtLedge(CallbackInfoReturnable<Boolean> cir) {
        if (!IsolatorState.isIsolating()) {
            return;
        }
        if ((Object) this instanceof LocalPlayer clientPlayer) {
            boolean isSneaking = clientPlayer.isShiftKeyDown();
            boolean returned = cir.getReturnValue();
            // In vanilla, isStayingOnGroundSurface strictly equals isShiftKeyDown()
            if (returned != isSneaking) {
                cir.setReturnValue(isSneaking);
                MovementIsolator.detectAndRecordCallerMod();
            }
        }
    }

    @Inject(method = "blockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void mrModded2Vanilla$clampBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (!IsolatorState.isIsolating()) {
            return;
        }
        if ((Object) this instanceof LocalPlayer clientPlayer) {
            double current = cir.getReturnValue();
            cir.setReturnValue(InteractionIsolator.sanitizeBlockInteractionRange(clientPlayer, current));
        }
    }

    @Inject(method = "entityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void mrModded2Vanilla$clampEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (!IsolatorState.isIsolating()) {
            return;
        }
        if ((Object) this instanceof LocalPlayer) {
            double current = cir.getReturnValue();
            cir.setReturnValue(InteractionIsolator.sanitizeEntityInteractionRange(current));
        }
    }
}
