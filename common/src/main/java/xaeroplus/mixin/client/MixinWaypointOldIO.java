package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.io.WaypointOldIO;
import xaeroplus.settings.Settings;

import java.nio.file.Path;

@Mixin(value = WaypointOldIO.class, remap = false)
public class MixinWaypointOldIO {
    @WrapOperation(method = "load", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/waypoint/io/WaypointOldIO;loadFromFile(Lxaero/hud/minimap/module/MinimapSession;Ljava/nio/file/Path;)Z"
    ))
    public boolean skipLoadingFromConfigFile(final WaypointOldIO instance, final MinimapSession session, final Path configFile, final Operation<Boolean> original) {
        if (Settings.REGISTRY.fixWaypointLoad.get()) {
            // i have no idea why he is checking xaerominimap.txt config file for waypoints
            // its not the old waypoints file, its only for settings
            // when it returns true, this triggers a full unnecessary save of all waypoints in the world
            // which has its own issues with multiple mc instance concurrency and can cause data loss
            return false;
        } else {
            return original.call(instance, session, configFile);
        }
    }
}
