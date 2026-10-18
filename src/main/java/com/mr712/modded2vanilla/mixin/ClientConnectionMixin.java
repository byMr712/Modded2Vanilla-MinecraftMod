package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.config.SelectKnownPacksC2SPacket;
import net.minecraft.registry.VersionedIdentifier;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ClientConnection.class, priority = 2000)
public abstract class ClientConnectionMixin {

    @Shadow
    public abstract void send(Packet<?> packet, ChannelFutureListener callbacks, boolean flush);

    @Inject(
        method = "send(Lnet/minecraft/network/packet/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mrModded2Vanilla$filterOutgoingCustomPayload(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.isInSingleplayer()) {
            return;
        }

        if (packet instanceof SelectKnownPacksC2SPacket packsPacket) {
            if (!IsolatorState.isModdedServer(this) && packsPacket.knownPacks() != null) {
                List<VersionedIdentifier> filtered = packsPacket.knownPacks().stream()
                    .filter(VersionedIdentifier::isVanilla)
                    .toList();
                if (filtered.size() != packsPacket.knownPacks().size()) {
                    ci.cancel();
                    this.send(new SelectKnownPacksC2SPacket(filtered), callbacks, flush);
                }
            }
            return;
        }

        if (packet instanceof CustomPayloadC2SPacket customPacket) {
            CustomPayload payload = customPacket.payload();
            if (payload != null && payload.getId() != null) {
                Identifier channelId = payload.getId().id();
                if (channelId != null && !IsolatorState.isChannelSupportedByServer(this, channelId)) {
                    MrModded2Vanilla.LOGGER.info("[MrModded2Vanilla] Suppressed unsupported outgoing CustomPayload channel: {}", channelId);
                    AdjustmentTracker.recordMod(channelId.getNamespace());
                    ci.cancel();
                }
            }
        }
    }
}
