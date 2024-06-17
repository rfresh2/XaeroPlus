package xaeroplus.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record DimensionSwitchEvent(ResourceKey<Level> dimension) { }
