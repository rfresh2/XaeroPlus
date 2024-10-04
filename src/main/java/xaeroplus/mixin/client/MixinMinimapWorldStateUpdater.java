package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.world.state.MinimapWorldStateUpdater;
import xaero.hud.path.XaeroPath;
import xaeroplus.util.DataFolderResolveUtil;

@Mixin(value = MinimapWorldStateUpdater.class, remap = false)
public class MixinMinimapWorldStateUpdater {

    @Inject(method = "getAutoRootContainerPath(I)Lxaero/hud/path/XaeroPath;", at = @At("HEAD"), cancellable = true)
    public void customDataFolderResolve(final int version, final CallbackInfoReturnable<XaeroPath> cir) {
        CallbackInfoReturnable<String> customCir = new CallbackInfoReturnable<String>("a", true);
        DataFolderResolveUtil.resolveDataFolder(customCir);
        if (customCir.isCancelled()) {
            cir.setReturnValue(XaeroPath.root(customCir.getReturnValue()));
        }
    }
}
