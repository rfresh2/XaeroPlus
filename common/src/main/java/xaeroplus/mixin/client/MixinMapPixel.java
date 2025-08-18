package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.region.MapPixel;
import xaeroplus.settings.Settings;

@Mixin(value = MapPixel.class, remap = false)
public abstract class MixinMapPixel {

    @Shadow
    protected BlockState state;

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
