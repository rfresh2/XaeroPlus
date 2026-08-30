package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.dropdown.rightclick.RightClickOption;
import xaero.map.gui.util.GuiUtils;
import xaero.map.mods.gui.Waypoint;
import xaero.map.mods.gui.WaypointReader;
import xaeroplus.settings.Settings;
import xaeroplus.util.BaritoneExecutor;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.PlayerRotationHelper;

import java.util.ArrayList;

@Mixin(value = WaypointReader.class, remap = false)
public class MixinWaypointReader {

    @Inject(method = "getRightClickOptions(Lxaero/map/mods/gui/Waypoint;Lxaero/map/gui/IRightClickableElement;)Ljava/util/ArrayList;",
        at = @At("RETURN"))
    public void getRightClickOptionsReturn(final Waypoint element, final IRightClickableElement target, final CallbackInfoReturnable<ArrayList<RightClickOption>> cir) {
        if (!Settings.REGISTRY.worldMapUIAdditions.get()) return;
        final ArrayList<RightClickOption> options = cir.getReturnValue();
        int index = 3;
        options.add(index++, new RightClickOption("xaeroplus.gui.world_map.copy_coordinates", options.size(), target) {
            @Override
            public void onAction(final Screen screen) {
                Minecraft.getInstance().keyboardHandler.setClipboard(element.getX() + " " + element.getY() + " " + element.getZ());
            }
        });
        if (BaritoneHelper.isBaritonePresent()) {
            int goalX = Mth.floor(element.getRenderX() - 0.5);
            int goalZ = Mth.floor(element.getRenderZ() - 0.5);
            boolean isYPresent = element.isyIncluded();
            int goalY = isYPresent ? element.getY() : 64;
            options.add(index++,
                new RightClickOption("xaeroplus.gui.world_map.baritone_goal_here", options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        if (isYPresent) {
                            BaritoneExecutor.goal(goalX, goalY, goalZ);
                        } else {
                            BaritoneExecutor.goal(goalX, goalZ);
                        }
                    }
                }.setNameFormatArgs(GuiUtils.getBoundKeyComponent(Settings.REGISTRY.worldMapBaritoneGoalHereKeybindSetting.getKeyBinding())));
            options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_path_here", options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        if (isYPresent) {
                            BaritoneExecutor.path(goalX, goalY, goalZ);
                        } else {
                            BaritoneExecutor.path(goalX, goalZ);
                        }
                    }
                }.setNameFormatArgs(GuiUtils.getBoundKeyComponent(Settings.REGISTRY.worldMapBaritonePathHereKeybindSetting.getKeyBinding())));
            if (BaritoneHelper.isBaritoneElytraPresent()) {
                options.add(index++, new RightClickOption("xaeroplus.gui.world_map.baritone_elytra_here", options.size(), target) {
                    @Override
                    public void onAction(Screen screen) {
                        if (isYPresent) {
                            BaritoneExecutor.elytra(goalX, goalY, goalZ);
                        } else {
                            BaritoneExecutor.elytra(goalX, goalZ);
                        }
                    }
                }.setNameFormatArgs(GuiUtils.getBoundKeyComponent(Settings.REGISTRY.worldMapBaritoneElytraHereKeybindSetting.getKeyBinding())));
            }
        }
        options.add(index++, new RightClickOption("xaeroplus.gui.world_map.rotate_here", options.size(), target) {
            @Override
            public void onAction(Screen screen) {
                var mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.player == null) return;
                    if (mc.player.isFallFlying()) {
                        PlayerRotationHelper.rotatePlayerTo(element.getX(), element.getZ());
                    } else {
                        PlayerRotationHelper.rotatePlayerTo(element.getX(), element.isyIncluded() ? element.getY() : Mth.floor(mc.player.getY()), element.getZ());
                    }
                });
            }
        }.setNameFormatArgs(GuiUtils.getBoundKeyComponent(Settings.REGISTRY.worldMapRotateHereKeybindSetting.getKeyBinding())));

        if (Settings.REGISTRY.disableWaypointSharing.get()) {
            options.removeIf(option -> ((AccessorRightClickOption) option).invokeGetName().equals("gui.xaero_right_click_waypoint_share"));
        }

        if (Settings.REGISTRY.disableTeleportation.get()) {
            options.removeIf(option -> ((AccessorRightClickOption) option).invokeGetName().equals("gui.xaero_right_click_waypoint_teleport"));
        }
    }
}
