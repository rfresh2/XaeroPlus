package xaeroplus.mixin.client;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;
import xaeroplus.util.BaritoneHelper;

import java.util.ArrayList;

import static java.util.Arrays.asList;

@Mixin(value = WaypointReader.class, remap = false)
public class MixinWaypointReader {

    @Inject(method = "getRightClickOptions(Lxaero/map/mods/gui/Waypoint;Lxaero/map/gui/IRightClickableElement;)Ljava/util/ArrayList;",
        at = @At("RETURN"))
    public void getRightClickOptionsReturn(final Waypoint element, final IRightClickableElement target, final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (BaritoneHelper.isBaritonePresent()) {
            final ArrayList<RightClickOption> options = cir.getReturnValue();
            options.addAll(3, asList(
                new RightClickOption(I18n.translate("gui.world_map.baritone_goal_here"), options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(new GoalXZ(element.getX(), element.getZ()));
                    }
                },
                new RightClickOption(I18n.translate("gui.world_map.baritone_path_here"), options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(element.getX(), element.getZ()));
                    }
                }
            ));
        }
    }
}
