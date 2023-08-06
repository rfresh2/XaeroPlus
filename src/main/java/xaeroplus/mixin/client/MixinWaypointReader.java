package xaeroplus.mixin.client;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.IWaypointDimension;

import java.util.ArrayList;

import static java.util.Arrays.asList;

@Mixin(value = WaypointReader.class, remap = false)
public class MixinWaypointReader {

    @Inject(method = "getRightClickOptions(Lxaero/map/mods/gui/Waypoint;Lxaero/map/gui/IRightClickableElement;)Ljava/util/ArrayList;",
        at = @At("RETURN"))
    public void getRightClickOptionsReturn(final Waypoint element, final IRightClickableElement target, final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (BaritoneHelper.isBaritonePresent()) {
            final ArrayList<RightClickOption> options = cir.getReturnValue();
            int wpDimension = ((IWaypointDimension) element).getDimension();
            int playerDimension = ChunkUtils.getActualDimension();
            double dimDiv = 1.0;
            if (wpDimension != playerDimension) {
                if (wpDimension == -1 && playerDimension == 0) {
                    dimDiv = 8.0;
                } else if (wpDimension == 0 && playerDimension == -1) {
                    dimDiv = 0.125;
                }
            }
            int destX = (int) (element.getX() * dimDiv);
            int destZ = (int) (element.getZ() * dimDiv);
            options.addAll(3, asList(
                new RightClickOption(I18n.format("gui.world_map.baritone_goal_here"), options.size(), target) {
                    @Override
                    public void onAction(GuiScreen screen) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(new GoalXZ(destX, destZ));
                    }
                },
                new RightClickOption(I18n.format("gui.world_map.baritone_path_here"), options.size(), target) {
                    @Override
                    public void onAction(GuiScreen screen) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(element.getX(), element.getZ()));
                    }
                }
            ));
        }
    }
}
