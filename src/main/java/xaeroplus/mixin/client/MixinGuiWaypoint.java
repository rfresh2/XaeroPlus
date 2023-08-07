package xaeroplus.mixin.client;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.mods.gui.Waypoint;
import xaeroplus.util.IWaypointDimension;

@Mixin(value = Waypoint.class, remap = false)
public class MixinGuiWaypoint implements IWaypointDimension {
    @Shadow
    private Object original;

    @Override
    public RegistryKey<World> getDimension() {
        if (original instanceof xaero.common.minimap.waypoints.Waypoint) {
            return ((IWaypointDimension) original).getDimension();
        }
        return World.OVERWORLD;
    }

    @Override
    public void setDimension(final RegistryKey<World> dimension) {
        if (original instanceof xaero.common.minimap.waypoints.Waypoint) {
            ((IWaypointDimension) original).setDimension(dimension);
        }
    }
}
