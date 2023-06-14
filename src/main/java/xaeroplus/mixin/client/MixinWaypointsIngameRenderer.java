package xaeroplus.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.minimap.waypoints.render.WaypointsIngameRenderer;

import java.util.Objects;

import static net.minecraft.world.World.NETHER;
import static xaeroplus.util.Shared.customDimensionId;

@Mixin(value = WaypointsIngameRenderer.class, remap = false)
public class MixinWaypointsIngameRenderer {

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lxaero/common/minimap/waypoints/WaypointsManager;getDimensionDivision(Ljava/lang/String;)D"))
    public double redirectDimensionDivision(final WaypointsManager waypointsManager, final String worldContainerID) {
        if (worldContainerID != null && MinecraftClient.getInstance().world != null) {
            try {
                RegistryKey<World> dim = MinecraftClient.getInstance().world.getRegistryKey();
                if (!Objects.equals(dim, customDimensionId)) {
                    double currentDimDiv = Objects.equals(dim, -1) ? 8.0 : 1.0;
                    String dimPart = worldContainerID.substring(worldContainerID.lastIndexOf(47) + 1);
                    RegistryKey<World> dimKey = waypointsManager.getDimensionKeyForDirectoryName(dimPart);
                    double selectedDimDiv = dimKey == NETHER ? 8.0 : 1.0;
                    return currentDimDiv / selectedDimDiv;
                }
            } catch (final Exception e) {
                // fall through
            }
        }
        return waypointsManager.getDimensionDivision(worldContainerID);
    }
}
