package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.lib.client.level.block.BlockTextureColorUtils;
import xaero.map.region.MapPixel;
import xaeroplus.feature.render.async.AsyncTileBuffers;
import xaeroplus.settings.Settings;

@Mixin(value = MapPixel.class, remap = false)
public abstract class MixinMapPixel {

    @Shadow
    protected BlockState state;

    @WrapOperation(method = "getPixelColours", at = @At(
        value = "INVOKE",
        target = "Lxaero/lib/client/level/block/BlockTextureColorUtils;getBlockTextureColor(Lnet/minecraft/world/level/block/state/BlockState;ZLnet/minecraft/world/level/Level;Lnet/minecraft/core/Registry;Lnet/minecraft/core/BlockPos;)I"
    ))
    public int asyncWorkerBlockTextureColor(final BlockTextureColorUtils utils, final BlockState state, final boolean convert, final Level world,
                                            final Registry<Block> registry, final BlockPos pos, final Operation<Integer> original) {
        if (!AsyncTileBuffers.isWorker()) {
            return original.call(utils, state, convert, world, registry, pos);
        }
        var immutablePos = pos.immutable();
        return AsyncTileBuffers.textureColor(utils, state, () -> original.call(utils, state, convert, world, registry, immutablePos));
    }

    @Inject(method = "getPixelColours", at = @At("RETURN"), remap = false)
    public void getPixelColours(
            final CallbackInfo ci,
            @Local(argsOnly = true) final int[] result_dest
    ) {
        if (Settings.REGISTRY.transparentObsidianRoofSetting.get()) {
            if (state.getBlock() == Blocks.OBSIDIAN || state.getBlock() == Blocks.CRYING_OBSIDIAN) {
                result_dest[3] = Settings.REGISTRY.transparentObsidianRoofDarkeningSetting.getAsInt();
            } else if (state.getBlock() == Blocks.SNOW) {
                result_dest[3] = Settings.REGISTRY.transparentObsidianRoofSnowOpacitySetting.getAsInt();
            }
        }
    }
}
