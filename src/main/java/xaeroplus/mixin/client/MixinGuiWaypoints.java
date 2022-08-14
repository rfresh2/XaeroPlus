package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;

@Mixin(targets = "xaero.common.gui.GuiWaypoints$List", remap = false)
public abstract class MixinGuiWaypoints {

    @Inject(method = "drawWaypointSlot(Lxaero/common/minimap/waypoints/Waypoint;II)V", at = @At(value = "TAIL"), remap = false)
    public void drawWaypointSlot(Waypoint w, int x, int y, CallbackInfo ci) throws ClassNotFoundException {
        Entity renderViewEntity = Minecraft.getMinecraft().getRenderViewEntity();
        final double playerX = renderViewEntity.posX;
        final double playerZ = renderViewEntity.posZ;
        final double playerY = renderViewEntity.posY;
        final double dimensionDivision = GuiWaypoints.distanceDivided;
        final int wpX = w.getX(dimensionDivision);
        final int wpY = w.getY();
        final int wpZ = w.getZ(dimensionDivision);
        final double distance = Math.sqrt(Math.pow(playerX - wpX, 2) + Math.pow(playerY - wpY, 2) + Math.pow(playerZ - wpZ, 2));
        final FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        final String text = (int)distance + "m";
        fontRenderer.drawStringWithShadow(text, (float) ((x + 300) - fontRenderer.getStringWidth(text) / 2), y + 1, 0xFFFFFF);
    }
}
