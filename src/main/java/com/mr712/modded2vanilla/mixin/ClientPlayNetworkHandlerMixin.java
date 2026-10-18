package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin {

    @Inject(method = "onGameJoin", at = @At("TAIL"))
    private void mrModded2Vanilla$onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (IsolatorState.isMultiplayer()) {
            AdjustmentTracker.printNoticeIfAny();
        }
    }

    @Inject(method = "clearWorld", at = @At("HEAD"))
    private void mrModded2Vanilla$onClearWorld(CallbackInfo ci) {
        AdjustmentTracker.resetSessionNotice();
    }

    @Inject(method = "onOpenScreen", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$guardOpenScreen(OpenScreenS2CPacket packet, CallbackInfo ci) {
        if (!IsolatorState.isIsolating()) {
            return;
        }
        if (packet == null || packet.getScreenHandlerType() == null) {
            MrModded2Vanilla.LOGGER.debug("[MrModded2Vanilla] Suppressed invalid OpenScreenS2CPacket from server.");
            ci.cancel();
        }
    }

    @Inject(method = "onPlaySound", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$guardPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        if (!IsolatorState.isMultiplayer()) {
            return;
        }
        if (packet == null || packet.getSound() == null || packet.getSound().value() == null) {
            MrModded2Vanilla.LOGGER.debug("[MrModded2Vanilla] Suppressed invalid PlaySoundS2CPacket from server.");
            ci.cancel();
        }
    }

    @Inject(method = "onPlaySoundFromEntity", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$guardPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
        if (!IsolatorState.isMultiplayer()) {
            return;
        }
        if (packet == null || packet.getSound() == null || packet.getSound().value() == null) {
            MrModded2Vanilla.LOGGER.debug("[MrModded2Vanilla] Suppressed invalid PlaySoundFromEntityS2CPacket from server.");
            ci.cancel();
        }
    }

    @Inject(method = "onParticle", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$guardParticle(ParticleS2CPacket packet, CallbackInfo ci) {
        if (!IsolatorState.isMultiplayer()) {
            return;
        }
        if (packet == null || packet.getParameters() == null) {
            MrModded2Vanilla.LOGGER.debug("[MrModded2Vanilla] Suppressed invalid ParticleS2CPacket from server.");
            ci.cancel();
        }
    }

    @Inject(method = "onBlockEntityUpdate", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$guardBlockEntityUpdate(net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket packet, CallbackInfo ci) {
        if (!IsolatorState.isMultiplayer()) {
            return;
        }
        if (packet == null || packet.getBlockEntityType() == null) {
            MrModded2Vanilla.LOGGER.debug("[MrModded2Vanilla] Suppressed invalid BlockEntityUpdate packet from server.");
            ci.cancel();
        }
    }

    @Inject(method = "onOverlayMessage", at = @At("HEAD"))
    private void mrModded2Vanilla$logOverlayMessage(net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket packet, CallbackInfo ci) {
        if (packet != null && packet.text() != null) {
            MrModded2Vanilla.LOGGER.info("[Server Action Bar] {}", packet.text().getString());
        }
    }

    @Inject(method = "onTitle", at = @At("HEAD"))
    private void mrModded2Vanilla$logTitle(net.minecraft.network.packet.s2c.play.TitleS2CPacket packet, CallbackInfo ci) {
        if (packet != null && packet.text() != null) {
            MrModded2Vanilla.LOGGER.info("[Server Title] {}", packet.text().getString());
        }
    }

    @Inject(method = "onSubtitle", at = @At("HEAD"))
    private void mrModded2Vanilla$logSubtitle(net.minecraft.network.packet.s2c.play.SubtitleS2CPacket packet, CallbackInfo ci) {
        if (packet != null && packet.text() != null) {
            MrModded2Vanilla.LOGGER.info("[Server Subtitle] {}", packet.text().getString());
        }
    }
}
