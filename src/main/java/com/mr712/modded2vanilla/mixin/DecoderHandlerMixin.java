package com.mr712.modded2vanilla.mixin;

import com.mr712.modded2vanilla.MrModded2Vanilla;
import com.mr712.modded2vanilla.state.IsolatorState;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ProtocolSwapHandler;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PacketDecoder.class)
public abstract class DecoderHandlerMixin {

    @Shadow
    @Final
    private ProtocolInfo<?> protocolInfo;

    @Inject(method = "decode", at = @At("HEAD"), cancellable = true)
    private void mrModded2Vanilla$safeDecode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> objects, CallbackInfo ci) {
        if (!IsolatorState.isIsolating()) {
            return;
        }

        int readable = buf.readableBytes();
        if (readable == 0) {
            return;
        }

        ci.cancel();

        int readerIndex = buf.readerIndex();
        try {
            Packet<?> packet = (Packet<?>) this.protocolInfo.codec().decode(buf);
            if (buf.isReadable()) {
                int extra = buf.readableBytes();
                MrModded2Vanilla.LOGGER.debug("[MrModded2Vanilla] Suppressed {} extra trailing bytes in packet {}", extra, packet.getClass().getSimpleName());
                buf.skipBytes(extra);
            }
            objects.add(packet);
            ProtocolSwapHandler.handleInboundTerminalPacket(ctx, packet);
        } catch (Throwable t) {
            MrModded2Vanilla.LOGGER.warn("[MrModded2Vanilla] Suppressed packet decode error in network stream: {}", t.getMessage());
            // Skip the remaining bytes for this frame to prevent crashing channel
            buf.skipBytes(buf.readableBytes());
        }
    }
}
