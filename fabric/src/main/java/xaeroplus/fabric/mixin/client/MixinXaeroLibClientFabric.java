package xaeroplus.fabric.mixin.client;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.lib.client.XaeroLibClientFabric;

@Mixin(value = XaeroLibClientFabric.class, remap = false)
public class MixinXaeroLibClientFabric {
    @Redirect(method = "load", at = @At(
        value = "INVOKE",
        target = "Lnet/fabricmc/fabric/api/resource/ResourceManagerHelper;registerReloadListener(Lnet/fabricmc/fabric/api/resource/IdentifiableResourceReloadListener;)V"
    ))
    public void disableResourceListenerForDevIdkItBreaksRunClient(final ResourceManagerHelper instance, final IdentifiableResourceReloadListener identifiableResourceReloadListener) {
        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
            instance.registerReloadListener(identifiableResourceReloadListener);
        }
    }
}
