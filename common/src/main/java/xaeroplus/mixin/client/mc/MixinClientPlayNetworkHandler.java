package xaeroplus.mixin.client.mc;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ChunkBlockUpdateEvent;
import xaeroplus.event.ChunkBlocksUpdateEvent;
import xaeroplus.event.ChunkDataEvent;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
    @Shadow private ClientLevel level;

    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    public void onChunkData(final ClientboundLevelChunkWithLightPacket packet, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new ChunkDataEvent(level.getChunk(packet.getX(), packet.getZ())));
    }

    @Inject(method = "handleChunkBlocksUpdate", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/network/protocol/game/ClientboundSectionBlocksUpdatePacket;runUpdates(Ljava/util/function/BiConsumer;)V"
    ))
    public void onChunkBlocksUpdate(final ClientboundSectionBlocksUpdatePacket packet, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new ChunkBlocksUpdateEvent(packet));
    }

    @Inject(method = "handleBlockUpdate", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientLevel;setServerVerifiedBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)V"
    ))
    public void onBlockUpdate(final ClientboundBlockUpdatePacket packet, final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new ChunkBlockUpdateEvent(packet));
    }
}
