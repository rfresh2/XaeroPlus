package xaeroplus.mixin.client;

import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.state.MinimapWorldState;
import xaero.hud.minimap.world.state.MinimapWorldStateUpdater;
import xaero.hud.path.XaeroPath;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.DataFolderResolveUtil;

@Mixin(value = MinimapWorldStateUpdater.class, remap = false)
public class MixinMinimapWorldStateUpdater {

    @Unique private int currentDim = 0;

    @Inject(method = "update", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/world/state/MinimapWorldState;setAutoWorldPath(Lxaero/hud/path/XaeroPath;)V"
    ), locals = LocalCapture.CAPTURE_FAILHARD)
    public void preferOwWpSetCustomPathOnDimUpdate(final MinimapSession session, final CallbackInfo ci,
                                                   MinimapWorldState state,
                                                   XaeroPath oldAutoWorldPath,
                                                   XaeroPath potentialAutoContainerPath,
                                                   boolean worldmap,
                                                   String potentialAutoWorldNode,
                                                   XaeroPath autoWorldPath) {
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

    @Inject(method = "getAutoRootContainerPath", at = @At("HEAD"), cancellable = true)
    public void customDataFolderResolve(final int version, final NetHandlerPlayClient connection, final MinimapSession session, final CallbackInfoReturnable<XaeroPath> cir) {
        CallbackInfoReturnable<String> customCir = new CallbackInfoReturnable<String>("a", true);
        DataFolderResolveUtil.resolveDataFolder(customCir);
        if (customCir.isCancelled()) {
            cir.setReturnValue(XaeroPath.root(customCir.getReturnValue()));
        }
    }
}
