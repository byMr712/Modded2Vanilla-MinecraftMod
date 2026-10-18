package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import com.mr712.modded2vanilla.tracker.AdjustmentTracker;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerboundSelectKnownPacks;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.KnownPack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = Connection.class, priority = 2000)
public abstract class ClientConnectionMixin {

    @Shadow
    public abstract void send(Packet<?> packet, ChannelFutureListener listener, boolean flush);

    @Inject(
        method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void mrModded2Vanilla$filterOutgoingCustomPayload(Packet<?> packet, ChannelFutureListener callbacks, boolean flush, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.hasSingleplayerServer()) {
            return;
        }

        if (packet instanceof ServerboundSelectKnownPacks packsPacket) {
            if (packsPacket.knownPacks() != null) {
                List<KnownPack> filtered = packsPacket.knownPacks().stream()
                    .filter(KnownPack::isVanilla)
                    .toList();
                if (filtered.size() != packsPacket.knownPacks().size()) {
                    ci.cancel();
                    this.send(new ServerboundSelectKnownPacks(filtered), callbacks, flush);
                }
            }
            return;
        }

        if (packet instanceof ServerboundCustomPayloadPacket customPacket) {
            CustomPacketPayload payload = customPacket.payload();
            if (payload != null && payload.type() != null) {
                Identifier channelId = payload.type().id();
                if (channelId != null && !IsolatorState.isChannelSupportedByServer(channelId)) {
                    MrModded2Vanilla.LOGGER.info("[MrModded2Vanilla] Suppressed unsupported outgoing CustomPayload channel: {}", channelId);
                    AdjustmentTracker.recordMod(channelId.getNamespace());
                    ci.cancel();
                }
            }
        }
    }
}
