package xaeroplus.util;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalBlock;
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

    public static GoalBlock getBaritoneGoalBlock(int x, int y, int z) {
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
        return new GoalBlock(goalX, y, goalZ);
    }

    public static void goal(int x, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(getBaritoneGoalXZ(x, z));
    }

    public static void goal(int x, int y, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(getBaritoneGoalBlock(x, y, z));
    }

    public static void path(int x, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(getBaritoneGoalXZ(x, z));
    }

    public static void path(int x, int y, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(getBaritoneGoalBlock(x, y, z));
    }

    public static void elytra(int x, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        if (!BaritoneHelper.isBaritoneElytraPresent()) return;
        BaritoneAPI.getSettings().elytraTermsAccepted.value = true;
        var goal = getBaritoneGoalXZ(x, z);
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(goal);
        BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(goal);
    }

    public static void elytra(int x, int y, int z) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        if (!BaritoneHelper.isBaritoneElytraPresent()) return;
        BaritoneAPI.getSettings().elytraTermsAccepted.value = true;
        var goal = getBaritoneGoalBlock(x, y, z);
        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(goal);
        BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(goal);
    }
}
