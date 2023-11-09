package xaeroplus.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.AXaeroMinimap;
import xaero.common.graphics.CustomRenderTypes;
import xaero.common.gui.GuiWaypoints;
import xaero.common.minimap.waypoints.Waypoint;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import java.lang.reflect.Field;
import java.text.NumberFormat;

@Mixin(targets = "xaero.common.gui.GuiWaypoints$List", remap = false)
public abstract class MixinGuiWaypointsList {
    private GuiWaypoints thisGuiWaypoints;
    private AXaeroMinimap modMain;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(final GuiWaypoints this$0, final CallbackInfo ci) throws NoSuchFieldException, IllegalAccessException {
        thisGuiWaypoints = this$0;
        final Field modMainField = thisGuiWaypoints.getClass().getSuperclass().getDeclaredField("modMain");
        modMainField.setAccessible(true);
        this.modMain = (AXaeroMinimap) modMainField.get(thisGuiWaypoints);
    }

    /**
     * @author rfresh2
     * @reason search support
     */
    @Inject(method = "getWaypointCount", at = @At("HEAD"), cancellable = true)
    public void getWaypointCount(final CallbackInfoReturnable<Integer> cir) {
        try {
            int size = ((MixinGuiWaypointsAccessor) thisGuiWaypoints).getWaypointsSorted().size();
            cir.setReturnValue(size);
        } catch (final NullPointerException e) {
            // fall through
        }
    }

    /**
     * @author rfresh2
     * @reason search support
     */
    @Overwrite
    private Waypoint getWaypoint(int slotIndex) {
        Waypoint waypoint = null;
        if (slotIndex < ((MixinGuiWaypointsAccessor) thisGuiWaypoints).getWaypointsSorted().size()) {
            waypoint = ((MixinGuiWaypointsAccessor) thisGuiWaypoints).getWaypointsSorted().get(slotIndex);
        } else if (((MixinGuiWaypointsAccessor) thisGuiWaypoints).getWaypointsManager().getServerWaypoints() != null) {
            int serverWPIndex = slotIndex - ((MixinGuiWaypointsAccessor) thisGuiWaypoints).getWaypointsSorted().size();
            if (serverWPIndex < ((MixinGuiWaypointsAccessor) thisGuiWaypoints).getWaypointsManager().getServerWaypoints().size()) {
                waypoint = ((MixinGuiWaypointsAccessor) thisGuiWaypoints).getWaypointsManager().getServerWaypoints().get(serverWPIndex);
            }
        }
        return waypoint;
    }

    /**
     * @author rfresh2
     * @reason Add waypoint distance to right side, and move waypoint initials icon farther left to handle long waypoint names
     */
    @Overwrite
    public void drawWaypointSlot(DrawContext guiGraphics, Waypoint w, int x, int y) {
        MatrixStack matrixStack = guiGraphics.getMatrices();
        if (w != null) {
            matrixStack.push();
            matrixStack.translate(0.0F, 0.0F, 1.0F);
            final TextRenderer fontRenderer = MinecraftClient.getInstance().textRenderer;
            guiGraphics.drawCenteredTextWithShadow(
                    fontRenderer,
                    w.getLocalizedName()
                            + (
                            w.isDisabled()
                                    ? " §4" + I18n.translate("gui.xaero_disabled")
                                    : (w.isTemporary() ? " §4" + I18n.translate("gui.xaero_temporary") : "")
                    ),
                    x + 110,
                    y + 1,
                    16777215
            );
            int rectX = x - 30;
            int rectY = y + 6;
            if (w.isGlobal()) {
                guiGraphics.drawCenteredTextWithShadow(fontRenderer, "*", rectX - 25, rectY - 3, 16777215);
            }

            VertexConsumerProvider.Immediate renderTypeBuffers = modMain.getInterfaceRenderer().getCustomVertexConsumers().getBetterPVPRenderTypeBuffers();
            modMain
                    .getInterfaces()
                    .getMinimapInterface()
                    .getWaypointsGuiRenderer()
                    .drawIconOnGUI(
                            guiGraphics,
                            modMain.getInterfaces().getMinimapInterface().getMinimapFBORenderer().getHelper(),
                            w,
                            modMain.getSettings(),
                            rectX,
                            rectY,
                            renderTypeBuffers,
                            renderTypeBuffers.getBuffer(CustomRenderTypes.COLORED_WAYPOINTS_BGS)
                    );
            if (XaeroPlusSettingRegistry.showWaypointDistances.getValue()) {
                Entity renderViewEntity = MinecraftClient.getInstance().getCameraEntity();
                final double playerX = renderViewEntity.getX();
                final double playerZ = renderViewEntity.getZ();
                final double playerY = renderViewEntity.getY();
                final double dimensionDivision = GuiWaypoints.distanceDivided;
                final int wpX = w.getX(dimensionDivision);
                final int wpY = w.getY();
                final int wpZ = w.getZ(dimensionDivision);
                final double distance = Math.sqrt(Math.pow(playerX - wpX, 2) + Math.pow(playerY - wpY, 2) + Math.pow(playerZ - wpZ, 2));
                final String text = NumberFormat.getIntegerInstance().format(distance) + "m";
                guiGraphics.drawTextWithShadow(fontRenderer, text, x + 250, y + 1, 0xFFFFFF);
            }
            renderTypeBuffers.draw();
            matrixStack.pop();
        }
    }
}
