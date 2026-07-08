package xaeroplus.mixin.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.widget.Tooltip;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.MinimapGuiTexturedButton;
import xaeroplus.settings.Settings;

import java.util.ArrayList;

@Mixin(value = GuiWaypoints.class, remap = false)
public abstract class MixinGuiWaypoints extends ScreenBase {
    @Unique
    private MinimapGuiTexturedButton toggleAllButton;

    @Shadow
    private MinimapWorld displayedWorld;
    @Shadow
    private ArrayList<Waypoint> waypointsSorted;

    protected MixinGuiWaypoints(final Screen parent, final Screen escape, final Component titleIn) {
        super(parent, escape, titleIn);
    }

    @Shadow
    protected abstract boolean isOneSelected();

    @Shadow
    private void updateSortedList() {}

    @Inject(method = "init", at = @At("HEAD"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.toggleAllButton = new MinimapGuiTexturedButton(
            this.width / 2 + 182, this.height - 29,
            20, 20,
            2, 18,
            17, 17,
            Globals.guiTextures,
            (b) -> {
                waypointsSorted.stream().findFirst().ifPresent(firstWaypoint -> {
                    boolean firstIsEnabled = firstWaypoint.isDisabled();
                    waypointsSorted.forEach(waypoint -> waypoint.setDisabled(!firstIsEnabled));
                });
                updateSortedList();
            },
            () -> new Tooltip(Component.literal("[XP] ").append(Component.translatable("xaeroplus.gui.waypoints.toggle_enable_all"))),
            256, 256
        );
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return;
        this.addRenderableWidget(toggleAllButton);
    }

    @Redirect(method = "updateButtons", at = @At(value = "INVOKE", target = "Lxaero/common/gui/GuiWaypoints;isOneSelected()Z"))
    public boolean shareButtonRedirect(final GuiWaypoints instance) {
        if (Settings.REGISTRY.disableWaypointSharing.get()) return false;
        return isOneSelected();
    }
}
