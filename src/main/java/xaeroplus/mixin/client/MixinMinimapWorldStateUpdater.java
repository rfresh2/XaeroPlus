package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.state.MinimapWorldStateUpdater;
import xaero.hud.path.XaeroPath;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.DataFolderResolveUtil;

@Mixin(value = MinimapWorldStateUpdater.class, remap = false)
public class MixinMinimapWorldStateUpdater {

    @Unique private int currentDim = 0;
    @Shadow @Final private MinimapSession session;

    @Inject(method = "update()V", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/world/state/MinimapWorldState;setAutoWorldPath(Lxaero/hud/path/XaeroPath;)V"
    ))
    public void preferOwWpSetCustomPathOnDimUpdate(final CallbackInfo ci, @Local(name = "potentialAutoContainerPath") String potentialAutoWorldNode) {
        if (XaeroPlusSettingRegistry.owAutoWaypointDimension.getValue()) {
            int actualDimension = ChunkUtils.getActualDimension();
            if (actualDimension == -1 && currentDim != actualDimension) {
                XaeroPath overworldWpXaeroPath = session.getWorldState().getAutoRootContainerPath()
                    .resolve(session.getDimensionHelper().getDimensionDirectoryName(0))
                    .resolve(potentialAutoWorldNode); // todo: probably not quite correct with all multiworld configs
                session.getWorldState().setCustomWorldPath(overworldWpXaeroPath);
            }
        }
        currentDim = ChunkUtils.getActualDimension();
    }

    @Inject(method = "getAutoRootContainerPath(I)Lxaero/hud/path/XaeroPath;", at = @At("HEAD"), cancellable = true)
    public void customDataFolderResolve(final int version, final CallbackInfoReturnable<XaeroPath> cir) {
        CallbackInfoReturnable<String> customCir = new CallbackInfoReturnable<String>("a", true);
        DataFolderResolveUtil.resolveDataFolder(customCir);
        if (customCir.isCancelled()) {
            cir.setReturnValue(XaeroPath.root(customCir.getReturnValue()));
        }
    }
}
