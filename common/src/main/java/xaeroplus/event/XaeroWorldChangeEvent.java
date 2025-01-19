package xaeroplus.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record XaeroWorldChangeEvent(WorldChangeType worldChangeType, @Nullable ResourceKey<Level> from, @Nullable ResourceKey<Level> to) {
    public enum WorldChangeType {
        ENTER_WORLD,
        EXIT_WORLD,
        SWITCH_TO_ALT_DIMENSION,
        SWITCH_BACK_TO_ACTUAL_DIMENSION,
        MULTIWORLD_SWITCH
    }
}
