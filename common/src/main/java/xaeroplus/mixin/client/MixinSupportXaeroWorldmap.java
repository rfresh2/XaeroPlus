package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.common.minimap.render.MinimapRendererHelper;
import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.WorldMapSession;
import xaero.map.region.MapTileChunk;
import xaeroplus.Globals;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class MixinSupportXaeroWorldmap {
    @Inject(method = "drawMinimap", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/settings/ModSettings;getRegionCacheHashCode()I"
    ), remap = false)
    public void overrideRegionRange(
        final CallbackInfo ci,
        @Local(name = "mapX") int mapX,
        @Local(name = "mapZ") int mapZ,
        @Local(name = "minX") LocalIntRef minXRef,
        @Local(name = "maxX") LocalIntRef maxXRef,
        @Local(name = "minZ") LocalIntRef minZRef,
        @Local(name = "maxZ") LocalIntRef maxZRef
    ) {
        final int scaledSize = Globals.minimapScaleMultiplier * 4;
        minXRef.set((mapX >> 2) - scaledSize);
        maxXRef.set((mapX >> 2) + scaledSize);
        minZRef.set((mapZ >> 2) - scaledSize);
        maxZRef.set((mapZ >> 2) + scaledSize);
    }


    @WrapWithCondition(method = "renderChunks", at = @At(
        value = "INVOKE",
        target = "Lxaero/common/mods/SupportXaeroWorldmap;renderSlimeChunks(Lxaero/map/region/MapTileChunk;Ljava/lang/Long;IILcom/mojang/blaze3d/vertex/PoseStack;Lxaero/common/minimap/render/MinimapRendererHelper;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"
    ), remap = true) // $REMAP
    public boolean hideSlimeChunksWhileDimSwitched(SupportXaeroWorldmap instance, MapTileChunk chunk, Long seed, int drawX, int drawZ, PoseStack matrixStack, MinimapRendererHelper helper, VertexConsumer overlayBufferBuilder) {
        return Globals.getCurrentDimensionId() == Minecraft.getInstance().level.dimension();
    }

    @Inject(method = "tryToGetMultiworldId", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/WorldMapSession;getMapProcessor()Lxaero/map/MapProcessor;"
    ), cancellable = true, remap = false)
    public void preventPossibleNPE(
        final CallbackInfoReturnable<String> cir,
        @Local WorldMapSession session
    ) {
        // possible race condition where WM session is not initialized when client ticks start
        /**
         * Caused by: java.lang.NullPointerException: Cannot invoke "xaero.map.WorldMapSession.getMapProcessor()" because "worldmapSession" is null
         *     at xaero.common.mods.SupportXaeroWorldmap.tryToGetMultiworldId(SupportXaeroWorldmap.java:362)
         *     at xaero.common.minimap.waypoints.WaypointsManager.getNewAutoWorldID(WaypointsManager.java:194)
         *     at xaero.common.minimap.waypoints.WaypointsManager.updateWorldIds(WaypointsManager.java:381)
         *     at xaero.common.events.FMLEventHandler.handlePlayerTickStart(FMLEventHandler.java:40)
         *     at xaero.common.events.FMLCommonEventHandler.handlePlayerTickStart(FMLCommonEventHandler.java:21)
         *     at net.minecraft.class_1657.handler$dae000$xaerominimap$onTickStart(class_1657.java:6382)
         */
        if (session == null) {
            cir.setReturnValue(null);
        }
    }
}
