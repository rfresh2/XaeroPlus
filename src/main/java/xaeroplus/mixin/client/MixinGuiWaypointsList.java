package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.world.MinimapWorld;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.Globals;

import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;

@Mixin(targets = "xaero.common.gui.GuiWaypoints$List", remap = false)
public abstract class MixinGuiWaypointsList {
    private GuiWaypoints thisGuiWaypoints;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(GuiWaypoints this$0, CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        // god why make this an inner static class i hate these hacks
        thisGuiWaypoints = this$0;
    }

    @Unique
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
            final Field displayedWorldField = thisGuiWaypoints.getClass().getDeclaredField("displayedWorld");
            displayedWorldField.setAccessible(true);
            final MinimapWorld minimapWorld = (MinimapWorld) displayedWorldField.get(thisGuiWaypoints);
            if (minimapWorld.getContainer().getServerWaypointManager() != null) {
                for(Waypoint w : minimapWorld.getContainer().getServerWaypointManager().getWaypoints()) {
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

    @Inject(method = "drawWaypointSlot", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/Waypoint;isGlobal()Z"
    ), remap = false)
    public void shiftIconsLeft(Waypoint w, int x, int y, final CallbackInfo ci,
                               @Local(name = "rectX") LocalIntRef rectX) {
        rectX.set(rectX.get() - 30);
    }

    @Inject(method = "drawWaypointSlot", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/render/WaypointsGuiRenderer;drawIconOnGUI(Lxaero/common/minimap/render/MinimapRendererHelper;Lxaero/common/minimap/waypoints/Waypoint;Lxaero/common/settings/ModSettings;II)V"
    ))
    public void drawWaypointDistances(Waypoint w, int x, int y, final CallbackInfo ci) {
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
            final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
            thisGuiWaypoints.drawString(fontRenderer, text, x + 250, y + 1, 0xFFFFFF);
        }
    }
}
