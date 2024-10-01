package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.state.MinimapWorldState;
import xaero.hud.minimap.world.state.MinimapWorldStateUpdater;
import xaero.hud.path.XaeroPath;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.DataFolderResolveUtil;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

@Mixin(value = MinimapWorldStateUpdater.class, remap = false)
public abstract class MixinMinimapWorldStateUpdater {

    @Final @Shadow private MinimapSession session;
    @Final @Shadow private ClientPacketListener connection;
    @Unique private ResourceKey<Level> currentDim = OVERWORLD;

    @WrapOperation(method = "update()V", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/world/state/MinimapWorldState;setAutoWorldPath(Lxaero/hud/path/XaeroPath;)V"
    ))
    public void preferOverworldWpSetCustomPathOnDimUpdate(final MinimapWorldState instance, final XaeroPath autoWorldPath, final Operation<Void> original,
                                   @Local(name = "oldAutoWorldPath") XaeroPath oldAutoWorldPath,
                                   @Local(name = "potentialAutoWorldNode") String potentialAutoWorldNode) {
        original.call(instance, autoWorldPath);
        if (Settings.REGISTRY.owAutoWaypointDimension.get()) {
            ResourceKey<Level> actualDimension = ChunkUtils.getActualDimension();
            if (actualDimension == NETHER && currentDim != actualDimension) {
                XaeroPath overworldWpXaeroPath = session.getWorldState().getAutoRootContainerPath()
                    .resolve(session.getDimensionHelper().getDimensionDirectoryName(OVERWORLD))
                    .resolve(potentialAutoWorldNode); // todo: probably not quite correct with all multiworld configs
                session.getWorldState().setCustomWorldPath(overworldWpXaeroPath);
            }
        }
        currentDim = ChunkUtils.getActualDimension();
    }

    @Inject(method = "getAutoRootContainerPath(I)Lxaero/hud/path/XaeroPath;", at = @At("HEAD"), cancellable = true)
    public void customDataFolderResolve(final int version, final CallbackInfoReturnable<XaeroPath> cir) {
        var customCir = new CallbackInfoReturnable<String>("a", true);
        DataFolderResolveUtil.resolveDataFolder(connection, customCir);
        if (customCir.isCancelled()) {
            cir.setReturnValue(XaeroPath.root(customCir.getReturnValue()));
        }
    }
}
