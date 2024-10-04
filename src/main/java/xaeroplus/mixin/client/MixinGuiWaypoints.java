package xaeroplus.mixin.client;

import com.google.common.base.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.GuiWaypoints;
import xaero.common.gui.MySmallButton;
import xaero.common.gui.ScreenBase;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsSort;
import xaero.common.misc.KeySortableByOther;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.world.MinimapWorldManager;
import xaeroplus.util.Globals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

import static java.util.Objects.isNull;

@Mixin(value = GuiWaypoints.class, remap = false)
public class MixinGuiWaypoints extends ScreenBase {

    private final int TOGGLE_ALL_ID = 69;
    private final int SEARCH_ID = 70;
    @Shadow private MinimapWorld displayedWorld;
    @Shadow private ArrayList<Waypoint> waypointsSorted;
    @Shadow private MinimapSession session;
    @Shadow private MinimapWorldManager manager;
    @Shadow private boolean buttonClicked;
    @Shadow private ConcurrentSkipListSet<Integer> selectedListSet;
    private GuiTextField searchField;

    protected MixinGuiWaypoints(IXaeroMinimap modMain, GuiScreen parent, GuiScreen escape) {
        super(modMain, parent, escape);
    }

    @Inject(method = "initGui()V", at = @At("TAIL"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.searchField = new GuiTextField(SEARCH_ID, this.fontRenderer, this.width / 2 - 297, 32, 80, 20);
        this.searchField.setText("");
        this.searchField.setFocused(true);
        this.searchField.setCursorPosition(0);
        this.searchField.setSelectionPos(0);
        Keyboard.enableRepeatEvents(true);
        Globals.waypointsSearchFilter = "";
        // todo: this button is a bit larger than i want but cba to figure out exact size rn
        this.buttonList.add(new MySmallButton(TOGGLE_ALL_ID, this.width / 2 + 213, this.height - 53, I18n.format("gui.waypoints.toggle_enable_all")));
    }

    @Inject(method = "actionPerformed", at = @At("TAIL"), remap = true)
    public void actionPerformed(GuiButton b, CallbackInfo ci) {
        if (b.enabled) {
            if (b.id == TOGGLE_ALL_ID) {
                waypointsSorted.stream().findFirst().ifPresent(firstWaypoint -> {
                    boolean firstIsEnabled = firstWaypoint.isDisabled();
                    waypointsSorted.forEach(waypoint -> waypoint.setDisabled(!firstIsEnabled));
                });
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lxaero/common/gui/ScreenBase;mouseClicked(III)V", shift = At.Shift.AFTER), remap = true)
    public void mouseClickedInject(int x, int y, int b, CallbackInfo ci) {
        boolean dropDownClosed = this.openDropdown == null;
        if (!this.buttonClicked && dropDownClosed) {
            if (this.searchField.mouseClicked(x, y, b)) {
                this.searchField.setFocused(true);
                this.searchField.setCursorPositionEnd();
                this.searchField.setSelectionPos(0);
            } else {
                this.searchField.setFocused(false);
            }
        }
    }

    @Inject(method = "keyTyped", at = @At(value = "INVOKE", target = "Lxaero/common/gui/ScreenBase;keyTyped(CI)V", shift = At.Shift.AFTER), remap = true, cancellable = true)
    public void keyTypedInject(char c, int i, CallbackInfo ci) {
        if (searchField.isFocused()) {
            searchField.textboxKeyTyped(c, i);
            updateSearch();
            ci.cancel();
        }
    }

    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lxaero/common/gui/ScreenBase;drawScreen(IIF)V", shift = At.Shift.AFTER), remap = true)
    public void drawScreenInject(int x, int y, float f, CallbackInfo ci) {
        if (!this.searchField.isFocused() && this.searchField.getText().isEmpty()) {
            xaero.map.misc.Misc.setFieldText(this.searchField, I18n.format("gui.xaero_settings_search_placeholder", new Object[0]), -11184811);
            this.searchField.setCursorPosition(0);
        }
        this.searchField.drawTextBox();
        if (!this.searchField.isFocused()) {
            xaero.map.misc.Misc.setFieldText(this.searchField, Globals.waypointsSearchFilter);
        }
    }

    @Unique
    private void updateSearch() {
        if (this.searchField.isFocused()) {
            String newValue = this.searchField.getText();
            if (!Objects.equal(Globals.waypointsSearchFilter, newValue)) {
                Globals.waypointsSearchFilter = this.searchField.getText();
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
        Entity renderViewEntity = Minecraft.getMinecraft().getRenderViewEntity();
        Vec3d cameraPos = isNull(renderViewEntity)
                ? ActiveRenderInfo.getCameraPosition()
                : ActiveRenderInfo.getCameraPosition().add(renderViewEntity.posX, renderViewEntity.posY, renderViewEntity.posZ);
        Vec3d lookVector = isNull(renderViewEntity) ? new Vec3d(1.0, 0.0, 0.0) : renderViewEntity.getLookVec();
        if (Minecraft.getMinecraft().gameSettings.thirdPersonView == 2) {
            lookVector = lookVector.scale(-1.0);
        }

        final List<Waypoint> disabledWaypoints = new ArrayList<>();
        final List<Waypoint> enabledWaypoints = new ArrayList<>();
        for (Waypoint w : waypointsList) {
            if (w.isDisabled())
                disabledWaypoints.add(w);
            else
                enabledWaypoints.add(w);
        }
        if (!java.util.Objects.equals(Globals.waypointsSearchFilter, "")) {
            enabledWaypoints.removeIf(waypoint -> !waypoint.getName().toLowerCase().contains(Globals.waypointsSearchFilter.toLowerCase()));
            disabledWaypoints.removeIf(waypoint -> !waypoint.getName().toLowerCase().contains(Globals.waypointsSearchFilter.toLowerCase()));
        }
        this.waypointsSorted = new ArrayList<>();

        this.waypointsSorted.addAll(sortWaypoints(enabledWaypoints, sortType, cameraPos, lookVector));
        this.waypointsSorted.addAll(sortWaypoints(disabledWaypoints, sortType, cameraPos, lookVector));
    }

    private List<Waypoint> sortWaypoints(final List<Waypoint> waypointsList, WaypointsSort sortType, final Vec3d cameraPos, final Vec3d lookVector) {
        final ArrayList<Waypoint> waypointsSorted = new ArrayList<>();
        final ArrayList<KeySortableByOther<Waypoint>> sortableKeys = new ArrayList<>();
        for(Waypoint w : waypointsList) {
            Comparable sortVal = 0;
            switch (sortType) {
                case NONE:
                    break;
                case ANGLE:
                     sortVal = -w.getComparisonAngleCos(cameraPos, lookVector, GuiWaypoints.distanceDivided);
                     break;
                case COLOR:
                    sortVal = w.getWaypointColor();
                    break;
                case NAME:
                    sortVal = w.getComparisonName();
                    break;
                case SYMBOL:
                    sortVal = w.getInitials();
                    break;
                case DISTANCE:
                    sortVal = w.getComparisonDistance(cameraPos, GuiWaypoints.distanceDivided);
                    break;
            }
            sortableKeys.add(
                    new KeySortableByOther<>(
                            w,
                            sortVal));
        }
        Collections.sort(sortableKeys);
        for(KeySortableByOther<Waypoint> k : sortableKeys) {
            waypointsSorted.add(k.getKey());
        }
        if (this.displayedWorld.getRootConfig().isSortReversed()) {
            Collections.reverse(waypointsSorted);
        }
        return waypointsSorted;
    }
}
