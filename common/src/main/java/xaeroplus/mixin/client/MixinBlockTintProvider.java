package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.lib.client.level.block.BlockTextureColorUtils;
import xaero.map.biome.BlockTintProvider;
import xaeroplus.feature.render.async.AsyncTileBuffers;

@Mixin(value = BlockTintProvider.class, remap = false)
public abstract class MixinBlockTintProvider {
    @WrapOperation(method = "getBiomeColor", at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/client/level/block/BlockTextureColorUtils;getBlockTintIndex(Lnet/minecraft/world/level/block/state/BlockState;)I"
    ))
    public int asyncWorkerBlockTintIndex(final BlockTextureColorUtils utils, final BlockState state, final Operation<Integer> original) {
        if (!AsyncTileBuffers.isWorker()) {
            return original.call(utils, state);
        }
        return AsyncTileBuffers.tintIndex(state, () -> original.call(utils, state));
    }
}
