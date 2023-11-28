package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.IXaeroMinimap;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.Globals;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;

@Mixin(targets = "xaero.common.gui.GuiWaypoints$List", remap = false)
public abstract class MixinGuiWaypointsList {

    private IXaeroMinimap modMain;
    private GuiWaypoints thisGuiWaypoints;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(GuiWaypoints this$0, CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        // god why make this an inner static class i hate these hacks
        thisGuiWaypoints = this$0;
        final Field modMainField = this$0.getClass().getSuperclass().getDeclaredField("modMain");
        modMainField.setAccessible(true);
        this.modMain = (IXaeroMinimap) modMainField.get(this$0);
    }

    public ArrayList<Waypoint> getSearchFilteredWaypointList() {
        ArrayList<Waypoint> filteredWaypoints = new ArrayList<>();
        try {
            final Field waypointsSortedField = thisGuiWaypoints.getClass().getDeclaredField("waypointsSorted");
            waypointsSortedField.setAccessible(true);
            final ArrayList<Waypoint> waypointsSorted = (ArrayList<Waypoint>) waypointsSortedField.get(thisGuiWaypoints);
            for(Waypoint w : waypointsSorted) {
                if (w.getName().toLowerCase().contains(Globals.waypointsSearchFilter.toLowerCase())) {
                    filteredWaypoints.add(w);
                }
            }
            final Field waypointsManagerField = thisGuiWaypoints.getClass().getDeclaredField("waypointsManager");
            waypointsManagerField.setAccessible(true);
            final WaypointsManager waypointsManagerValue = (WaypointsManager) waypointsManagerField.get(thisGuiWaypoints);
            if (waypointsManagerValue.getServerWaypoints() != null) {
                for(Waypoint w : waypointsManagerValue.getServerWaypoints()) {
                    if (w.getName().toLowerCase().contains(Globals.waypointsSearchFilter.toLowerCase())) {
                        filteredWaypoints.add(w);
                    }
                }
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error filtering waypoints list", e);
        }

        return filteredWaypoints;
    }

    /**
     * @author rfresh2
     * @reason search support
     */
    @Overwrite(remap = true)
    public int getSize() {
        return getSearchFilteredWaypointList().size();
    }

    /**
     * @author rfresh2
     * @reason search support
     */
    @Overwrite
    private Waypoint getWaypoint(int slotIndex) {
        ArrayList<Waypoint> searchFilteredWaypointList = getSearchFilteredWaypointList();
        if (slotIndex < searchFilteredWaypointList.size()) {
            return searchFilteredWaypointList.get(slotIndex);
        } else {
            return null;
        }
    }

    /**
     * @author rfresh2
     * @reason Add waypoint distance to right side, and move waypoint initials icon farther left to handle long waypoint names
     */
    @Overwrite
    public void drawWaypointSlot(Waypoint w, int x, int y) {
            if (w != null) {
                final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
                thisGuiWaypoints.drawCenteredString(
                        fontRenderer,
                        w.getLocalizedName()
                                + (
                                w.isDisabled()
                                        ? " §4" + I18n.format("gui.xaero_disabled")
                                        : (w.isTemporary() ? " §4" + I18n.format("gui.xaero_temporary") : "")
                        ),
                        x + 110,
                        y + 1,
                        16777215
                );
                int rectX = x - 30;
                int rectY = y + 6;
                if (w.isGlobal()) {
                    thisGuiWaypoints.drawCenteredString(fontRenderer, "*", rectX - 25, rectY - 3, 16777215);
                }

                this.modMain
                        .getInterfaces()
                        .getMinimapInterface()
                        .getWaypointsGuiRenderer()
                        .drawIconOnGUI(
                                this.modMain.getInterfaces().getMinimapInterface().getMinimapFBORenderer().getHelper(),
                                w,
                                this.modMain.getSettings(),
                                rectX,
                                rectY
                        );
                if (XaeroPlusSettingRegistry.showWaypointDistances.getValue()) {
                    Entity renderViewEntity = Minecraft.getMinecraft().getRenderViewEntity();
                    final double playerX = renderViewEntity.posX;
                    final double playerZ = renderViewEntity.posZ;
                    final double playerY = renderViewEntity.posY;
                    final double dimensionDivision = GuiWaypoints.distanceDivided;
                    final int wpX = w.getX(dimensionDivision);
                    final int wpY = w.getY();
                    final int wpZ = w.getZ(dimensionDivision);
                    final double distance = Math.sqrt(Math.pow(playerX - wpX, 2) + Math.pow(playerY - wpY, 2) + Math.pow(playerZ - wpZ, 2));
                    final String text = NumberFormat.getIntegerInstance().format(distance) + "m";
                    thisGuiWaypoints.drawString(fontRenderer, text, x + 250, y + 1, 0xFFFFFF);
                }
            }
    }
}
