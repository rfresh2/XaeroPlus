package xaeroplus.mixin.client;

import net.minecraft.block.state.IBlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.cache.BlockStateColorTypeCache;
import xaeroplus.util.DelegatingHashTable;

import java.util.Hashtable;

@Mixin(value = BlockStateColorTypeCache.class, remap = false)
public class MixinBlockStateColorTypeCache {
    @Shadow
    private Hashtable<IBlockState, Integer> colorTypes;
    @Shadow
    private Hashtable<IBlockState, Object> defaultColorResolversCache;

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    public void injectConstructor(final CallbackInfo ci) {
        // brug why do u keep using hashtable when its slow as shit
        colorTypes = new DelegatingHashTable<>();
        defaultColorResolversCache = new DelegatingHashTable<>();
    }
}
