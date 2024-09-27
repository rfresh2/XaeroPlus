package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaeroplus.settings.Settings;

import java.text.NumberFormat;

@Mixin(targets = "xaero.common.gui.GuiWaypoints$List", remap = false)
public abstract class MixinGuiWaypointsList {
    @Unique private final WaypointSet waypointsSortedSet = WaypointSet.Builder.begin().setName("xp-wp-sorted").build();
    @Unique private GuiWaypoints thisGuiWaypoints;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final GuiWaypoints this$0, final CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        thisGuiWaypoints = this$0;
    }

    /**
     * @author rfresh2
     * @reason search support
     */
    @Inject(method = "getWaypointCount", at = @At("HEAD"), cancellable = true)
    public void getWaypointCount(final CallbackInfoReturnable<Integer> cir) {
        try {
            int size = ((AccessorGuiWaypoints) thisGuiWaypoints).getWaypointsSorted().size();
            cir.setReturnValue(size);
        } catch (final NullPointerException e) {
            // fall through
        }
    }

    @Redirect(method = "getWaypoint", at = @At(
        value = "INVOKE",
        target = "Lxaero/hud/minimap/world/MinimapWorld;getCurrentWaypointSet()Lxaero/hud/minimap/waypoint/set/WaypointSet;"
    ))
    public WaypointSet getWaypointList(final MinimapWorld instance) {
        // reusing waypoint set to avoid allocating an arraylist every call to this method
        waypointsSortedSet.clear();
        waypointsSortedSet.addAll(((AccessorGuiWaypoints) thisGuiWaypoints).getWaypointsSorted());
        return waypointsSortedSet;
    }

    @Inject(method = "drawWaypointSlot", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/minimap/waypoints/Waypoint;isGlobal()Z"
    ), remap = false)
    public void shiftIconsLeft(final GuiGraphics guiGraphics, final Waypoint w, final int x, final int y, final CallbackInfo ci,
                               @Local(name = "rectX") LocalIntRef rectX) {
        rectX.set(rectX.get() - 30);
    }

    @Inject(method = "drawWaypointSlot", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;endBatch()V"
    ), remap = true)
    public void drawWaypointDistances(final GuiGraphics guiGraphics, final Waypoint w, final int x, final int y, final CallbackInfo ci) {
        if (Settings.REGISTRY.showWaypointDistances.get()) {
            Entity renderViewEntity = Minecraft.getInstance().getCameraEntity();
            final double playerX = renderViewEntity.getX();
            final double playerZ = renderViewEntity.getZ();
            final double playerY = renderViewEntity.getY();
            final double dimensionDivision = GuiWaypoints.distanceDivided;
            final int wpX = w.getX(dimensionDivision);
            final int wpY = w.getY();
            final int wpZ = w.getZ(dimensionDivision);
            final double distance = Math.sqrt(Math.pow(playerX - wpX, 2) + Math.pow(playerY - wpY, 2) + Math.pow(playerZ - wpZ, 2));
            final String text = NumberFormat.getIntegerInstance().format(distance) + "m";
            final Font fontRenderer = Minecraft.getInstance().font;
            guiGraphics.drawString(fontRenderer, text, x + 250, y + 1, 0xFFFFFF);
        }
    }
}
