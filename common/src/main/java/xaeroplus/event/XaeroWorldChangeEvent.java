package xaeroplus.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record XaeroWorldChangeEvent(WorldChangeType worldChangeType, @Nullable ResourceKey<Level> from, @Nullable ResourceKey<Level> to) {
    public enum WorldChangeType {
        ENTER_WORLD,
        EXIT_WORLD,
        ACTUAL_DIMENSION_SWITCH,
        VIEWED_DIMENSION_SWITCH,
        MULTIWORLD_SWITCH
    }
}
