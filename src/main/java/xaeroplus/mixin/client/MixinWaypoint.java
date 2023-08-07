package xaeroplus.mixin.client;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import xaero.common.minimap.waypoints.Waypoint;
import xaeroplus.util.IWaypointDimension;

@Mixin(value = Waypoint.class, remap = false)
public class MixinWaypoint implements IWaypointDimension {

    private RegistryKey<World> dimension = null;

    @Override
    public RegistryKey<World> getDimension() {
        return dimension;
    }

    @Override
    public void setDimension(RegistryKey<World> dimension) {
        this.dimension = dimension;
    }
}
