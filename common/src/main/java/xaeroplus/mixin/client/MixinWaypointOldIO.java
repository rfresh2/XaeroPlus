package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.io.WaypointOldIO;
import xaeroplus.settings.Settings;

import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(value = WaypointOldIO.class, remap = false)
public class MixinWaypointOldIO {
    @WrapOperation(method = "load", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/waypoint/io/WaypointOldIO;loadFromFile(Lxaero/hud/minimap/module/MinimapSession;Ljava/nio/file/Path;)Z"
    ))
    public boolean skipLoadingFromConfigFile(final WaypointOldIO instance, final MinimapSession session, final Path configFile, final Operation<Boolean> original) {
        if (Settings.REGISTRY.fixWaypointLoad.get()
            && configFile.getFileName().toString().endsWith("xaerominimap.txt")
            // ideally we wouldn't do any IO here, but i'd rather play this very safe
            && !xaeroPlus$configFileMaybeContainsWayponts(configFile)
        ) {
            // i have no idea why he is checking xaerominimap.txt config file for waypoints
            // its not the old waypoints file, its only for settings
            // original logic always returns true even when it contains no waypoints
            // which triggers a full unnecessary save of ALL waypoints in EVERY world
            // mass resave has issues with multiple mc instance concurrency and can possibly cause data loss
            return false;
        } else {
            return original.call(instance, session, configFile);
        }
    }

    @Unique
    private boolean xaeroPlus$configFileMaybeContainsWayponts(Path configFile) {
        try {
            if (!Files.exists(configFile)) {
                return false;
            }
            try (var lineStream = Files.lines(configFile)) {
                return lineStream.anyMatch(line -> line.startsWith("waypoint:") || line.startsWith("world:"));
            }
        } catch (Exception e) {
            return true;
        }
    }
}
