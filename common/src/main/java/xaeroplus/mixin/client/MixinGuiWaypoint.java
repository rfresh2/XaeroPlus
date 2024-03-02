package xaeroplus.mixin.client;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.mods.gui.Waypoint;
import xaeroplus.feature.extensions.IWaypointDimension;

@Mixin(value = Waypoint.class, remap = false)
public class MixinGuiWaypoint implements IWaypointDimension {
    @Shadow
    private Object original;

    @Override
    public ResourceKey<Level> getDimension() {
        if (original instanceof xaero.common.minimap.waypoints.Waypoint) {
            return ((IWaypointDimension) original).getDimension();
        }
        return Level.OVERWORLD;
    }

    @Override
    public void setDimension(final ResourceKey<Level> dimension) {
        if (original instanceof xaero.common.minimap.waypoints.Waypoint) {
            ((IWaypointDimension) original).setDimension(dimension);
        }
    }
}
