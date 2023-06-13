package xaeroplus.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.Shared;

@Mixin(value = MapProcessor.class, remap = false)
public class MixinMapProcessor {

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true)
    private void getMainId(final boolean rootFolderFormat, final ClientPlayNetworkHandler connection, final CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
    }

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true)
    public void getDimensionName(final RegistryKey<World> id, final CallbackInfoReturnable<String> cir) {
        if (!Shared.nullOverworldDimensionFolder) {
            if (id == World.OVERWORLD) {
                cir.setReturnValue("DIM0");
            }
        }
    }
}
