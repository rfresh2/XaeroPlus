package xaeroplus.mixin.client.mc;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;

@Mixin(ClientPlayNetworkHandler.class)
public class MixinClientPlayNetworkHandler {
    @Shadow private ClientWorld world;

    @Inject(method = "onChunkData", at = @At("RETURN"))
    public void onChunkData(final ChunkDataS2CPacket packet, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.dispatch(new ChunkDataEvent(world.getChunk(packet.getChunkX(), packet.getChunkZ())));
    }
}
