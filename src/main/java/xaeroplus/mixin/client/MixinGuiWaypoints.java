package xaeroplus.mixin.client;

import com.google.common.base.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.render.Camera;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.AXaeroMinimap;
import xaero.common.gui.GuiWaypoints;
import xaero.common.gui.MySmallButton;
import xaero.common.gui.ScreenBase;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.minimap.waypoints.WaypointsSort;
import xaero.common.misc.KeySortableByOther;
import xaeroplus.util.Shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Mixin(value = GuiWaypoints.class, remap = false)
public class MixinGuiWaypoints extends ScreenBase {

    private final int TOGGLE_ALL_ID = 69;
    private final int SEARCH_ID = 70;
    @Shadow
    private WaypointWorld displayedWorld;
    @Shadow
    private ArrayList<Waypoint> waypointsSorted;
    @Shadow
    private WaypointsManager waypointsManager;
    private TextFieldWidget searchField;
    private MySmallButton toggleAllButton;

    protected MixinGuiWaypoints(final AXaeroMinimap modMain, final Screen parent, final Screen escape, final Text titleIn) {
        super(modMain, parent, escape, titleIn);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    public void initGui(CallbackInfo ci) {
        this.searchField = new TextFieldWidget(this.textRenderer, this.width / 2 - 297, 32, 80, 20, Text.literal("Search"));
        this.searchField.setText("");
        this.searchField.setFocused(true);
        this.searchField.setCursor(0, false);
        this.searchField.setSelectionStart(0);
        this.addSelectableChild(searchField);
        this.setFocused(this.searchField);

        Shared.waypointsSearchFilter = "";
        // todo: this button is a bit larger than i want but cba to figure out exact size rn
        this.addDrawableChild(
                this.toggleAllButton = new MySmallButton(
                        TOGGLE_ALL_ID,
                        this.width / 2 + 213,
                        this.height - 53,
                        Text.translatable("gui.waypoints.toggle_enable_all"),
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
                this.searchField.setCursorToEnd(false);
                this.searchField.setEditable(true);
//                this.searchField.setSelectionEnd(0);
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

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lxaero/common/gui/ScreenBase;render(Lnet/minecraft/client/gui/DrawContext;IIF)V", shift = At.Shift.AFTER), remap = true)
    public void drawScreenInject(final DrawContext guiGraphics, final int mouseX, final int mouseY, final float partial, final CallbackInfo ci) {
        if (!this.searchField.isFocused() && this.searchField.getText().isEmpty()) {
            xaero.map.misc.Misc.setFieldText(this.searchField, I18n.translate("gui.xaero_settings_search_placeholder", new Object[0]), -11184811);
            this.searchField.setCursorToStart(false);
        }
        this.searchField.render(guiGraphics, mouseX, mouseY, partial);
        if (!this.searchField.isFocused()) {
            xaero.map.misc.Misc.setFieldText(this.searchField, Shared.waypointsSearchFilter);
        }
    }

    private void updateSearch() {
        if (this.searchField.isFocused()) {
            String newValue = this.searchField.getText();
            if (!Objects.equal(Shared.waypointsSearchFilter, newValue)) {
                Shared.waypointsSearchFilter = this.searchField.getText();
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
        WaypointsSort sortType = this.displayedWorld.getContainer().getRootContainer().getSortType();
        ArrayList<Waypoint> waypointsList = this.displayedWorld.getCurrentSet().getList();
        GuiWaypoints.distanceDivided = this.waypointsManager.getDimensionDivision(this.displayedWorld.getContainer().getKey());
        Camera camera = this.client.gameRenderer.getCamera();

        final List<Waypoint> disabledWaypoints = waypointsList.stream()
                .filter(Waypoint::isDisabled)
                .collect(Collectors.toList());
        final List<Waypoint> enabledWaypoints = waypointsList.stream()
                .filter(waypoint -> !waypoint.isDisabled())
                .collect(Collectors.toList());
        if (!java.util.Objects.equals(Shared.waypointsSearchFilter, "")) {
            enabledWaypoints.removeIf(waypoint -> !waypoint.getName().toLowerCase().contains(Shared.waypointsSearchFilter.toLowerCase()));
            disabledWaypoints.removeIf(waypoint -> !waypoint.getName().toLowerCase().contains(Shared.waypointsSearchFilter.toLowerCase()));
        }
        this.waypointsSorted = new ArrayList<>();

        this.waypointsSorted.addAll(sortWaypoints(enabledWaypoints, sortType, camera));
        this.waypointsSorted.addAll(sortWaypoints(disabledWaypoints, sortType, camera));
    }

    private List<Waypoint> sortWaypoints(final List<Waypoint> waypointsList, WaypointsSort sortType, final Camera camera) {
        final ArrayList<Waypoint> waypointsSorted = new ArrayList<>();
        final ArrayList<KeySortableByOther<Waypoint>> sortableKeys = new ArrayList<>();
        for (Waypoint w : waypointsList) {
            Comparable sortVal = 0;
            switch (sortType) {
                case NONE:
                    break;
                case ANGLE:
                    sortVal = -w.getComparisonAngleCos(camera, GuiWaypoints.distanceDivided);
                    break;
                case COLOR:
                    sortVal = w.getColor();
                    break;
                case NAME:
                    sortVal = w.getComparisonName();
                    break;
                case SYMBOL:
                    sortVal = w.getSymbol();
                    break;
                case DISTANCE:
                    sortVal = w.getComparisonDistance(camera, GuiWaypoints.distanceDivided);
                    break;
            }
            sortableKeys.add(
                    new KeySortableByOther<>(
                            w,
                            sortVal));
        }
        Collections.sort(sortableKeys);
        for (KeySortableByOther<Waypoint> k : sortableKeys) {
            waypointsSorted.add(k.getKey());
        }
        if (this.displayedWorld.getContainer().getRootContainer().isSortReversed()) {
            Collections.reverse(waypointsSorted);
        }
        return waypointsSorted;
    }
}
