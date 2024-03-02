package xaeroplus.mixin.client;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;
import xaeroplus.feature.extensions.IWaypointDimension;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.ChunkUtils;

import java.util.ArrayList;

import static java.util.Arrays.asList;
import static net.minecraft.world.level.Level.NETHER;
import static net.minecraft.world.level.Level.OVERWORLD;

@Mixin(value = WaypointReader.class, remap = false)
public class MixinWaypointReader {

    @Inject(method = "getRightClickOptions(Lxaero/map/mods/gui/Waypoint;Lxaero/map/gui/IRightClickableElement;)Ljava/util/ArrayList;",
        at = @At("RETURN"))
    public void getRightClickOptionsReturn(final Waypoint element, final IRightClickableElement target, final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (BaritoneHelper.isBaritonePresent()) {
            final ArrayList<RightClickOption> options = cir.getReturnValue();
            ResourceKey<Level> wpDimension = ((IWaypointDimension) element).getDimension();
            if (wpDimension == null) {
                wpDimension = OVERWORLD;
            }
            ResourceKey<Level> playerDimension = ChunkUtils.getActualDimension();
            double dimDiv = 1.0;
            if (wpDimension != playerDimension) {
                if (wpDimension == NETHER && playerDimension == OVERWORLD) {
                    dimDiv = 8.0;
                } else if (wpDimension == OVERWORLD && playerDimension == NETHER) {
                    dimDiv = 0.125;
                }
            }
            int destX = (int) (element.getX() * dimDiv);
            int destZ = (int) (element.getZ() * dimDiv);
            options.addAll(3, asList(
                new RightClickOption(I18n.get("gui.world_map.baritone_goal_here"), options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoal(new GoalXZ(destX, destZ));
                    }
                },
                new RightClickOption(I18n.get("gui.world_map.baritone_path_here"), options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ(destX, destZ));
                    }
                }
            ));
            if (BaritoneHelper.isBaritoneElytraPresent()) {
                options.addAll(5, asList(
                    new RightClickOption(I18n.get("gui.world_map.baritone_elytra_here"), options.size(), target) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess().pathTo(new GoalXZ(destX, destZ));
                        }
                    }
                ));
            }
        }

        if (XaeroPlusSettingRegistry.disableWaypointSharing.getValue()) {
            cir.getReturnValue().removeIf(option -> ((AccessorRightClickOption) option).getName().equals("gui.xaero_right_click_waypoint_share"));
        }
    }
}
