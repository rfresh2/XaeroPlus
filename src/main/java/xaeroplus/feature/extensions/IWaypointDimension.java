package xaeroplus.feature.extensions;

import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public interface IWaypointDimension {
    RegistryKey<World> getDimension();
    void setDimension(RegistryKey<World> dimension);
}
