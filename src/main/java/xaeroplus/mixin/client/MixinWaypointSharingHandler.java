package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import xaero.common.minimap.waypoints.WaypointSharingHandler;

@Mixin(value = WaypointSharingHandler.class, remap = false)
public class MixinWaypointSharingHandler {
}
