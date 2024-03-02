package xaeroplus.util;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.core.BlockPos;

/**
 * Only need this class because we cannot have a method with the Goal return type in BaritoneGoalSync
 * The EventBus will not find the method and throw if Baritone isn't present
 */
public class BaritoneGoalHelper {
    public static Goal getBaritoneGoal() {
        if (BaritoneHelper.isBaritoneElytraPresent()) {
            BlockPos elytraGoalPos = BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().currentDestination();
            if (elytraGoalPos != null) {
                return new GoalXZ(elytraGoalPos.getX(), elytraGoalPos.getZ());
            }
        }
        return BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().getGoal();
    }
}
