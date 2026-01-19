package xaeroplus.util;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.calc.IPath;
import baritone.process.ElytraProcess;
import baritone.process.elytra.ElytraBehavior;
import net.minecraft.core.BlockPos;
import xaeroplus.XaeroPlus;

import java.util.Collections;
import java.util.List;

public class BaritonePathHelper {

    public static List<BlockPos> getBaritonePath() {
        if (BaritoneHelper.isBaritoneElytraPresent() && BaritoneHelper.isBaritoneDeobf() && BaritoneHelper.isElytraPathAccessible()) {
            var iElytraProcess = BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess();
            var elytraGoalPos = iElytraProcess.currentDestination();
            if (elytraGoalPos != null) {
                return getElytraPath();
            }
        }

        return BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().getPath()
            .map(IPath::positions)
            .map(bpsList -> (List<BlockPos>) ((List) bpsList))
            .orElse(Collections.emptyList());
    }

    static List<BlockPos> getElytraPath() {
        try {
            var elytraProcess = (ElytraProcess) BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess();
            var behaviorField = elytraProcess.getClass().getDeclaredField("behavior");
            behaviorField.setAccessible(true);
            var elytraBehavior = (ElytraBehavior) behaviorField.get(elytraProcess);
            if (elytraBehavior == null) return Collections.emptyList();
            var pathManager = elytraBehavior.pathManager;
            if (pathManager == null) return Collections.emptyList();
            var path = pathManager.getPath();
            if (path == null) return Collections.emptyList();
            return (List<BlockPos>) (List) path;
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("Failed getting elytra path", e);
            return Collections.emptyList();
        }
    }
}
