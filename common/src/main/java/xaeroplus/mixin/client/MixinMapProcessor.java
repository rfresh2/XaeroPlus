package xaeroplus.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.extensions.CustomMapProcessor;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.DataFolderResolveUtil;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor implements CustomMapProcessor {
    @Shadow private String currentWorldId;
    @Shadow private String currentDimId;
    @Shadow private String currentMWId;
    @Unique private String xaeroPlus$prevWorldId;
    @Unique private String xaeroPlus$prevDimId;
    @Unique private String xaeroPlus$prevMWId;
    @Unique private final static String LOCK_ID = UUID.randomUUID().toString();
    @Shadow private ClientLevel world;

    @Unique
    private static final ThreadLocal<Boolean> xaeroPlus$getLeafRegionActualDimSignal = ThreadLocal.withInitial(() -> false);
    @Unique
    private static final ThreadLocal<Boolean> xaeroPlus$getCurrentDimensionActualDimSignal = ThreadLocal.withInitial(() -> false);
    @Override
    public ThreadLocal<Boolean> xaeroPlus$getLeafRegionActualDimSignal() {
        return xaeroPlus$getLeafRegionActualDimSignal;
    }
    @Override
    public ThreadLocal<Boolean> xaeroPlus$getCurrentDimensionActualDimSignal() {
        return xaeroPlus$getLeafRegionActualDimSignal;
    }

    @Shadow public abstract String getDimensionName(final ResourceKey<Level> id);

    @Inject(method = "getMainId(ILnet/minecraft/client/multiplayer/ClientPacketListener;)Ljava/lang/String;", at = @At("HEAD"),
        cancellable = true,
        remap = true) // $REMAP
    private void getMainId(final int version, final ClientPacketListener connection, final CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
    }

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void getDimensionName(final ResourceKey<Level> id, final CallbackInfoReturnable<String> cir) {
        if (!Globals.nullOverworldDimensionFolder) {
            if (id == Level.OVERWORLD) {
                cir.setReturnValue("DIM0");
            }
        }
    }

    @Redirect(method = "run", at = @At(
        value = "INVOKE",
        target = "Ljava/lang/Thread;sleep(J)V"
    ))
    public void decreaseThreadSleepTime(final long millis) throws InterruptedException {
        Thread.sleep(5L);
    }

    @Redirect(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"
    ))
    public Path replaceLockPath(final Path instance, final String other) {
        return Paths.get(System.getProperty("java.io.tmpdir")).resolve(LOCK_ID + ".lock");
    }

    @Inject(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/MapProcessor;popRenderPause(ZZ)V",
        ordinal = 0
    ))
    public void fireWorldChangedEvent(final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new XaeroWorldChangeEvent(this.currentWorldId, this.currentDimId, this.currentMWId));
    }

    @Inject(method = "getCurrentDimension", at = @At("HEAD"), cancellable = true)
    public void getActualDimIfSignalSet(final CallbackInfoReturnable<String> cir) {
        if (xaeroPlus$getCurrentDimensionActualDimSignal.get()) {
            cir.setReturnValue(getDimensionName(ChunkUtils.getActualDimension()));
        }
    }

    @WrapOperation(method = "getLeafMapRegion", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;",
        ordinal = 0
    ))
    public MapDimension getLeafMapRegionActualDimensionIfSignalled(final MapWorld instance, final Operation<MapDimension> original) {
        var world = this.world;
        if (xaeroPlus$getLeafRegionActualDimSignal().get() && world != null && xaeroPlus$prevDimId != null && xaeroPlus$prevDimId.equals(getDimensionName(world.dimension()))) {
            return instance.getDimension(world.dimension());
        } else return original.call(instance);
    }

    @Redirect(method = "getLeafMapRegion", at = @At(
        value = "NEW",
        target = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lxaero/map/world/MapDimension;IIIIZLnet/minecraft/core/Registry;)Lxaero/map/region/MapRegion;"
    ), remap = true) // $REMAP
    public MapRegion createMapRegionInActualDimensionIfSignalled(String worldId, String dimId, String mwId, final MapDimension dim, final int x, final int z, final int caveLayer, final int initialVersion, final boolean normalMapData, final Registry biomeRegistry) {
        var world = this.world;
        if (xaeroPlus$getLeafRegionActualDimSignal().get() && world != null && xaeroPlus$prevDimId != null && xaeroPlus$prevDimId.equals(getDimensionName(world.dimension()))) {
            worldId = xaeroPlus$prevWorldId;
            dimId = xaeroPlus$prevDimId;
            mwId = xaeroPlus$prevMWId;
        }
        return new MapRegion(
            worldId,
            dimId,
            mwId,
            dim,
            x,
            z,
            caveLayer,
            initialVersion,
            normalMapData,
            biomeRegistry);
    }

    @WrapOperation(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/world/MapWorld;getCurrentDimension()Lxaero/map/world/MapDimension;",
        ordinal = 0
    ),
    slice = @Slice(
        from = @At(
            value = "INVOKE",
            target = "Lxaero/map/MapProcessor;releaseLocksIfNeeded()V",
            ordinal = 0
        )
    ))
    public MapDimension updateWorldSyncedGetActualDimension(final MapWorld mapWorld, final Operation<MapDimension> original) {
        var world = this.world;
        return Settings.REGISTRY.writesWhileDimSwitched.get() && world != null && mapWorld.isMultiplayer()
            ? mapWorld.getDimension(world.dimension())
            : original.call(mapWorld);
    }

    @WrapOperation(method = "updateWorldSynced", at = @At(
        value = "FIELD",
        target = "Lxaero/map/MapProcessor;currentWorldId:Ljava/lang/String;",
        opcode = Opcodes.PUTFIELD,
        ordinal = 0
    ))
    public void storePrevWorldVarStates(final MapProcessor instance, final String value, final Operation<Void> original) {
        var world = this.world;
        if (world != null && getDimensionName(world.dimension()).equals(currentDimId)) {
            this.xaeroPlus$prevWorldId = this.currentWorldId;
            this.xaeroPlus$prevDimId = this.currentDimId;
            this.xaeroPlus$prevMWId = this.currentMWId;
        }
        original.call(instance, value);
    }
}
