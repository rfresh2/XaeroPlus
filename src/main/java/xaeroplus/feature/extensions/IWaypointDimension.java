package xaeroplus.feature.extensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public interface IWaypointDimension {
    ResourceKey<Level> getDimension();
    void setDimension(ResourceKey<Level> dimension);
}
