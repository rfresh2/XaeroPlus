package xaeroplus.mixin.client.mc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkDataEvent;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
    @Shadow private ClientLevel level;

    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    public void onChunkData(final ClientboundLevelChunkWithLightPacket packet, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new ChunkDataEvent(this.level.getChunk(packet.getX(), packet.getZ())));
    }
}
