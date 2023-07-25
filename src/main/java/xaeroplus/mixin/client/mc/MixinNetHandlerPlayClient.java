package xaeroplus.mixin.client.mc;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketChunkData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {
    @Shadow
    private WorldClient world;

    @Inject(method = "handleChunkData", at = @At("RETURN"))
    public void handleChunkData(final SPacketChunkData packetIn, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.post(new ChunkDataEvent(packetIn.isFullChunk(), this.world.getChunk(packetIn.getChunkX(), packetIn.getChunkZ())));
    }
}
