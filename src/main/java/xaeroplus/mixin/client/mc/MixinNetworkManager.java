package xaeroplus.mixin.client.mc;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.NewChunks;

@Mixin(value = NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("RETURN"))
    private void channelReadPost(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callbackInfo) {
        NewChunks.handlePacketEvent(packet);
    }
}
