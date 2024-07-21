package xaeroplus.util;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaeroplus.Globals;

import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

// avoid classloading this unless baritone is actually present
// otherwise game crashes
public final class BaritoneExecutor {
    private BaritoneExecutor() {}
    public static GoalXZ getBaritoneGoalXZ(int x, int z) {
        ResourceKey<Level> customDim = Globals.getCurrentDimensionId();
        ResourceKey<Level> actualDim = ChunkUtils.getActualDimension();
        double customDimDiv = 1.0;
        if (customDim != actualDim) {
            if (customDim == NETHER && actualDim == OVERWORLD) {
                customDimDiv = 8;
            } else if (customDim == OVERWORLD && actualDim == NETHER) {
                customDimDiv = 0.125;
            }
        }
        int goalX = (int) (x * customDimDiv);
        int goalZ = (int) (z * customDimDiv);
        return new GoalXZ(goalX, goalZ);
    }

    public static void goal(int x, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(getBaritoneGoalXZ(x, z));
    }

    public static void path(int x, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(getBaritoneGoalXZ(x, z));
    }

    public static void elytra(int x, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        if (!BaritoneHelper.isBaritoneElytraPresent()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(getBaritoneGoalXZ(x, z));
    }
}
