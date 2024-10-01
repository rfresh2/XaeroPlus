package xaeroplus.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.GuiWaypoints;
import xaero.common.gui.MySmallButton;
import xaero.common.gui.ScreenBase;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsSort;
import xaero.common.misc.KeySortableByOther;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaeroplus.settings.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Mixin(value = GuiWaypoints.class, remap = false)
public abstract class MixinGuiWaypoints extends ScreenBase {
    @Unique private EditBox searchField;
    @Unique private final int TOGGLE_ALL_ID = 69;
    @Unique private MySmallButton toggleAllButton;
    @Unique private String waypointsSearchFilter = "";

    @Shadow private MinimapWorld displayedWorld;
    @Shadow private ArrayList<Waypoint> waypointsSorted;
    @Shadow protected abstract boolean isOneSelected();
    @Shadow private MinimapSession session;
    @Shadow private ConcurrentSkipListSet<Integer> selectedListSet;

    protected MixinGuiWaypoints(final IXaeroMinimap modMain, final Screen parent, final Screen escape, final Component titleIn) {
        super(modMain, parent, escape, titleIn);
    }

    @Inject(method = "init", at = @At("RETURN"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.searchField = new EditBox(this.font, this.width / 2 - 297, 32, 80, 20, Component.literal("Search"));
        this.searchField.setValue("");
        this.searchField.setFocused(true);
        this.searchField.moveCursorTo(0, false);
        this.searchField.setCursorPosition(0);
        this.addWidget(searchField);
        this.setFocused(this.searchField);

        this.waypointsSearchFilter = "";
        // todo: this button is a bit larger than i want but cba to figure out exact size rn
        this.addRenderableWidget(
                this.toggleAllButton = new MySmallButton(
                        TOGGLE_ALL_ID,
                        this.width / 2 + 213,
                        this.height - 53,
                        Component.translatable("xaeroplus.gui.waypoints.toggle_enable_all"),
                        b -> {
                            waypointsSorted.stream().findFirst().ifPresent(firstWaypoint -> {
                                boolean firstIsEnabled = firstWaypoint.isDisabled();
                                waypointsSorted.forEach(waypoint -> waypoint.setDisabled(!firstIsEnabled));
                            });
                        }));
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lxaero/common/gui/ScreenBase;mouseClicked(DDI)Z", shift = At.Shift.AFTER), remap = true)
    public void mouseClickedInject(final double x, final double y, final int button, final CallbackInfoReturnable<Boolean> cir) {
        boolean dropDownClosed = this.openDropdown == null;
        if (dropDownClosed) {
            if (this.searchField.mouseClicked(x, y, button)) {
                this.searchField.setFocused(true);
                this.searchField.moveCursorToEnd(false);
                this.searchField.setEditable(true);
            } else {
                this.searchField.setFocused(false);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lxaero/common/gui/ScreenBase;keyPressed(III)Z", shift = At.Shift.AFTER), remap = true, cancellable = true)
    public void keyTypedInject(final int keycode, final int scanCode, final int modifiers, final CallbackInfoReturnable<Boolean> cir) {
        if (searchField.isFocused()) {
            updateSearch();
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean charTyped(char c, int i) {
        boolean result = super.charTyped(c, i);
        updateSearch();
        return result;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lxaero/common/gui/ScreenBase;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER), remap = true)
    public void drawScreenInject(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partial, final CallbackInfo ci) {
        if (!this.searchField.isFocused() && this.searchField.getValue().isEmpty()) {
            xaero.map.misc.Misc.setFieldText(this.searchField, I18n.get("xaeroplus.gui.xaero_settings_search_placeholder", new Object[0]), -11184811);
            this.searchField.moveCursorToStart(false);
        }
        this.searchField.render(guiGraphics, mouseX, mouseY, partial);
        if (!this.searchField.isFocused()) {
            xaero.map.misc.Misc.setFieldText(this.searchField, this.waypointsSearchFilter);
        }
    }

    @Redirect(method = "updateButtons", at = @At(value = "INVOKE", target = "Lxaero/common/gui/GuiWaypoints;isOneSelected()Z"))
    public boolean shareButtonRedirect(final GuiWaypoints instance) {
        if (Settings.REGISTRY.disableWaypointSharing.get()) return false;
        return isOneSelected();
    }

    @Unique
    private void updateSearch() {
        if (this.searchField.isFocused()) {
            String newValue = this.searchField.getValue();
            if (!this.waypointsSearchFilter.equals(newValue)) {
                this.waypointsSearchFilter = this.searchField.getValue();
                selectedListSet.clear();
                updateSortedList();
            }
        }
    }

    /**
     * @author rfresh2
     * @reason Always sort enabled waypoints before disabled waypoints
     */
    @Overwrite
    private void updateSortedList() {
        WaypointsSort sortType = this.displayedWorld.getRootConfig().getSortType();
        Iterable<Waypoint> waypointsList = this.displayedWorld.getCurrentWaypointSet().getWaypoints();
        GuiWaypoints.distanceDivided = this.session.getDimensionHelper().getDimensionDivision(this.displayedWorld);
        Camera camera = this.minecraft.gameRenderer.getMainCamera();

        final List<Waypoint> disabledWaypoints = new ArrayList<>();
        final List<Waypoint> enabledWaypoints = new ArrayList<>();
        for (Waypoint w : waypointsList) {
            if (w.isDisabled())
                disabledWaypoints.add(w);
             else
                 enabledWaypoints.add(w);
        }
        if (!this.waypointsSearchFilter.isEmpty()) {
            enabledWaypoints.removeIf(waypoint -> !waypoint.getName().toLowerCase().contains(this.waypointsSearchFilter.toLowerCase()));
            disabledWaypoints.removeIf(waypoint -> !waypoint.getName().toLowerCase().contains(this.waypointsSearchFilter.toLowerCase()));
        }
        this.waypointsSorted = new ArrayList<>();
        this.waypointsSorted.addAll(sortWaypoints(enabledWaypoints, sortType, camera));
        this.waypointsSorted.addAll(sortWaypoints(disabledWaypoints, sortType, camera));
    }

    @Unique
    private List<Waypoint> sortWaypoints(final List<Waypoint> waypointsList, WaypointsSort sortType, final Camera camera) {
        final ArrayList<Waypoint> waypointsSorted = new ArrayList<>();
        final ArrayList<KeySortableByOther<Waypoint>> sortableKeys = new ArrayList<>();
        for (Waypoint w : waypointsList) {
            Comparable sortVal = 0;
            switch (sortType) {
                case NONE -> {}
                case ANGLE -> sortVal = -w.getComparisonAngleCos(camera, GuiWaypoints.distanceDivided);
                case COLOR -> sortVal = w.getWaypointColor();
                case NAME -> sortVal = w.getComparisonName();
                case SYMBOL -> sortVal = w.getInitials();
                case DISTANCE -> sortVal = w.getComparisonDistance(camera, GuiWaypoints.distanceDivided);
            }
            sortableKeys.add(new KeySortableByOther<>(w, sortVal));
        }
        Collections.sort(sortableKeys);
        for (KeySortableByOther<Waypoint> k : sortableKeys) {
            waypointsSorted.add(k.getKey());
        }
        if (this.displayedWorld.getContainer().getRootConfig().isSortReversed()) {
            Collections.reverse(waypointsSorted);
        }
        return waypointsSorted;
    }
}
