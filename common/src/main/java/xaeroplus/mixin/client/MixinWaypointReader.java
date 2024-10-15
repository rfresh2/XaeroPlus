package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.misc.Misc;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;
import xaeroplus.settings.Settings;
import xaeroplus.util.BaritoneExecutor;
import xaeroplus.util.BaritoneHelper;

import java.util.ArrayList;

import static java.util.Arrays.asList;

@Mixin(value = WaypointReader.class, remap = false)
public class MixinWaypointReader {

    @Inject(method = "getRightClickOptions(Lxaero/map/mods/gui/Waypoint;Lxaero/map/gui/IRightClickableElement;)Ljava/util/ArrayList;",
        at = @At("RETURN"))
    public void getRightClickOptionsReturn(final Waypoint element, final IRightClickableElement target, final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        final ArrayList<RightClickOption> options = cir.getReturnValue();
        int index = 3;
        options.add(index++, new RightClickOption("xaeroplus.gui.world_map.copy_coordinates", options.size(), target) {
            @Override
            public void onAction(final Screen screen) {
                Minecraft.getInstance().keyboardHandler.setClipboard(element.getX() + " " + element.getY() + " " + element.getZ());
            }
        });
        if (BaritoneHelper.isBaritonePresent()) {
            int goalX = element.getX();
            int goalZ = element.getZ();
            options.add(index++,
                new RightClickOption("xaeroplus.gui.world_map.baritone_goal_here", options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        BaritoneExecutor.goal(goalX, goalZ);
                    }
                }.setNameFormatArgs(Misc.getKeyName(Settings.REGISTRY.worldMapBaritoneGoalHereKeybindSetting.getKeyBinding())));
            options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_path_here", options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        BaritoneExecutor.path(goalX, goalZ);
                    }
                }.setNameFormatArgs(Misc.getKeyName(Settings.REGISTRY.worldMapBaritonePathHereKeybindSetting.getKeyBinding())));
            if (BaritoneHelper.isBaritoneElytraPresent()) {
                options.addAll(5, asList(
                    new RightClickOption("xaeroplus.gui.world_map.baritone_elytra_here", options.size(), target) {
                        @Override
                        public void onAction(Screen screen) {
                            BaritoneExecutor.elytra(goalX, goalZ);
                        }
                    }.setNameFormatArgs(Misc.getKeyName(Settings.REGISTRY.worldMapBaritoneElytraHereKeybindSetting.getKeyBinding()))
                ));
            }
        }

        if (Settings.REGISTRY.disableWaypointSharing.get()) {
            cir.getReturnValue().removeIf(option -> ((AccessorRightClickOption) option).getName().equals("xaeroplus.gui.xaero_right_click_waypoint_share"));
        }
    }
}
