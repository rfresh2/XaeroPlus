package xaeroplus.fabric.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import xaero.lib.client.XaeroLibClientFabric;

@Mixin(value = XaeroLibClientFabric.class, remap = false)
public class MixinXaeroLibClientFabric {
//    @Redirect(method = "load", at = @At(
//        value = "INVOKE",
//        target = "Lnet/fabricmc/fabric/api/resource/ResourceManagerHelper;registerReloadListener(Lnet/fabricmc/fabric/api/resource/IdentifiableResourceReloadListener;)V"
//    ))
//    public void disableResourceListenerForDevIdkItBreaksRunClient(final ResourceManagerHelper instance, final IdentifiableResourceReloadListener identifiableResourceReloadListener) {
//        if (!FabricLoader.getInstance().isDevelopmentEnvironment()) {
//            instance.registerReloadListener(identifiableResourceReloadListener);
//        }
//    }
}
