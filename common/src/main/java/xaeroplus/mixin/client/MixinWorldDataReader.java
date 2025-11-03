package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapTileChunk;
import xaeroplus.settings.Settings;

import static net.minecraft.world.level.Level.NETHER;

@Mixin(value = WorldDataReader.class, remap = false)
public abstract class MixinWorldDataReader {

    @Inject(method = "buildTile", at = @At("HEAD"))
    public void applyNetherCaveFix(
        final CallbackInfoReturnable<Boolean> cir,
        @Local(argsOnly = true) MapTileChunk tileChunk,
        @Local(argsOnly = true, ordinal = 4) LocalIntRef caveStartRef
    ) {
        if (!Settings.REGISTRY.netherCaveFix.get()) return;
        boolean cave = caveStartRef.get() != Integer.MAX_VALUE;
        boolean nether = tileChunk.getInRegion().getDim().getDimId() == NETHER;
        if (!cave && nether) {
            caveStartRef.set(Integer.MIN_VALUE);
        }
    }
}
