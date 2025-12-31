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
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsSort;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.lib.client.gui.ScreenBase;
import xaero.lib.client.gui.util.GuiUtils;
import xaero.lib.client.gui.widget.Tooltip;
import xaero.lib.common.util.KeySortableByOther;
import xaeroplus.Globals;
import xaeroplus.feature.extensions.MinimapGuiTexturedButton;
import xaeroplus.settings.Settings;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

@Mixin(value = GuiWaypoints.class, remap = false)
public abstract class MixinGuiWaypoints extends ScreenBase {
    @Unique private EditBox searchField;
    @Unique private MinimapGuiTexturedButton toggleAllButton;
    @Unique private String waypointsSearchFilter = "";

    @Shadow private MinimapWorld displayedWorld;
    @Shadow private ArrayList<Waypoint> waypointsSorted;

    protected MixinGuiWaypoints(final Screen parent, final Screen escape, final Component titleIn) {
        super(parent, escape, titleIn);
    }

    @Shadow protected abstract boolean isOneSelected();
    @Shadow private MinimapSession session;
    @Shadow private ConcurrentSkipListSet<Integer> selectedListSet;

    @Override
    public void tick() {
        super.tick();
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return;
        this.searchField.tick();
    }

    @Inject(method = "init", at = @At("HEAD"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.waypointsSearchFilter = "";
        this.searchField = new EditBox(this.font,
            this.width / 2 - 208, 64, 50, 20,
            Component.translatable("xaeroplus.gui.waypoints.search"));
        this.searchField.setValue("");
        this.searchField.moveCursorTo(0);
        this.searchField.setCursorPosition(0);
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
            () -> new Tooltip(Component.literal("[XP] ").append(Component.translatable("xaeroplus.gui.waypoints.toggle_enable_all")))
    );
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return;
        if (this.width > 590) {
            this.searchField.setX(this.width / 2 - 280);
            this.searchField.setWidth(64);
            this.searchField.setY(32);
        }
        this.addWidget(searchField);
        this.setFocused(this.searchField);
        this.addRenderableWidget(toggleAllButton);
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lxaero/lib/client/gui/ScreenBase;mouseClicked(DDI)Z", shift = At.Shift.AFTER), remap = true)
    public void mouseClickedInject(final double x, final double y, final int button, final CallbackInfoReturnable<Boolean> cir) {
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return;
        boolean dropDownClosed = this.openDropdown == null;
        if (dropDownClosed) {
            if (this.searchField.mouseClicked(x, y, button)) {
                setFocused(this.searchField);
                this.searchField.moveCursorToEnd();
                this.searchField.setEditable(true);
            } else {
                this.searchField.setFocused(false);
            }
        }
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lxaero/lib/client/gui/ScreenBase;keyPressed(III)Z", shift = At.Shift.AFTER), remap = true, cancellable = true)
    public void keyTypedInject(final int keycode, final int scanCode, final int modifiers, final CallbackInfoReturnable<Boolean> cir) {
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return;
        if (searchField.isFocused() && searchField.isVisible()) {
            updateSearch();
            cir.setReturnValue(true);
        }
    }

    @Override
    public boolean charTyped(char c, int i) {
        boolean result = super.charTyped(c, i);
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return result;
        updateSearch();
        return result;
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lxaero/lib/client/gui/ScreenBase;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", shift = At.Shift.AFTER), remap = true)
    public void drawScreenInject(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partial, final CallbackInfo ci) {
        if (!Settings.REGISTRY.waypointsListUIAdditions.get()) return;
        if (!this.searchField.isFocused() && this.searchField.getValue().isEmpty()) {
            GuiUtils.setFieldText(this.searchField, I18n.get("gui.xaero_settings_search_placeholder", new Object[0]), ColorHelper.getColor(85, 85, 85, 255));
            this.searchField.moveCursorToStart();
        }
        this.searchField.render(guiGraphics, mouseX, mouseY, partial);
        if (!this.searchField.isFocused()) {
            GuiUtils.setFieldText(this.searchField, this.waypointsSearchFilter);
        }
        super.renderTooltips(guiGraphics, mouseX, mouseY, partial);
    }

    @Redirect(method = "updateButtons", at = @At(value = "INVOKE", target = "Lxaero/common/gui/GuiWaypoints;isOneSelected()Z"))
    public boolean shareButtonRedirect(final GuiWaypoints instance) {
        if (Settings.REGISTRY.disableWaypointSharing.get()) return false;
        return isOneSelected();
    }

    @Unique
    private void updateSearch() {
        String newValue = this.searchField.getValue();
        if (!this.waypointsSearchFilter.equals(newValue)) {
            this.waypointsSearchFilter = this.searchField.getValue();
            selectedListSet.clear();
            updateSortedList();
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
