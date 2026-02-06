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

import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

import static xaeroplus.event.XaeroWorldChangeEvent.WorldChangeType.*;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor implements CustomMapProcessor {
    @Unique private String xaeroPlus$prevWorldId;
    @Unique private String xaeroPlus$prevDimId;
    @Unique private String xaeroPlus$prevMWId;

    @Unique private boolean xaeroPlus$worldChange_prevMapWorldUsable;
    @Unique private WeakReference<ClientLevel> xaeroPlus$worldChange_prevWorld;
    @Unique private String xaeroPlus$worldChange_prevCurrentMWId;
    @Unique private ResourceKey<Level> xaeroPlus$worldChange_prevMapWorldCurrentDimId;
    @Unique private boolean xaeroPlus$nextWorldChangeIsDimSwitch = false;
    @Unique private final static String LOCK_ID = UUID.randomUUID().toString();

    @Shadow private ClientLevel world;
    @Shadow private MapWorld mapWorld;
    @Shadow private boolean mapWorldUsable;
    @Shadow private String currentWorldId;
    @Shadow private String currentDimId;
    @Shadow private String currentMWId;

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

    @Inject(method = "onWorldUnload", at = @At("HEAD"))
    public void resetCustomDimOnWorldUnload(final CallbackInfo ci) {
        // Fixes a bug in base mods where if a custom viewed dimension is set,
        // and the player changes dimensions, the same custom viewed dimension will persist
        if (this.mapWorld != null) {
            this.mapWorld.setCustomDimensionId(null);
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
        target = "Lxaero/map/MapProcessor;pushRenderPause(ZZ)V",
        ordinal = 0
    ))
    public void capturePrevStateForWorldChangeEvent(CallbackInfo ci) {
        this.xaeroPlus$worldChange_prevMapWorldUsable = this.mapWorldUsable;
        this.xaeroPlus$worldChange_prevWorld = new WeakReference<>(this.world);
        this.xaeroPlus$worldChange_prevCurrentMWId = this.currentMWId;
        this.xaeroPlus$worldChange_prevMapWorldCurrentDimId = this.mapWorld != null ? this.mapWorld.getCurrentDimensionId() : null;
    }

    @Inject(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/MapProcessor;popRenderPause(ZZ)V",
        ordinal = 0
    ))
    public void fireWorldChangedEvent(final CallbackInfo ci) {
        if (Globals.switchingDimension) {
            XaeroPlus.LOGGER.info("Skipping dim switch world change event firing");
            xaeroPlus$nextWorldChangeIsDimSwitch = true;
            return;
        }
        boolean dimSwitching = this.xaeroPlus$nextWorldChangeIsDimSwitch;
        this.xaeroPlus$nextWorldChangeIsDimSwitch = false;
        var mapWorldDim = this.mapWorld != null ? this.mapWorld.getCurrentDimensionId() : null;
        XaeroWorldChangeEvent.WorldChangeType type;
        ResourceKey<Level> from, to;
        if (!xaeroPlus$worldChange_prevMapWorldUsable && mapWorldUsable && xaeroPlus$worldChange_prevWorld.get() == null && !dimSwitching) {
            type = ENTER_WORLD;
            from = null;
            to = mapWorldDim;
        } else if (xaeroPlus$worldChange_prevMapWorldUsable && !mapWorldUsable && world == null) {
            type = EXIT_WORLD;
            from = mapWorldDim;
            to = null;
        } else if (dimSwitching) {
            type = ACTUAL_DIMENSION_SWITCH;
            from = null;
            to = mapWorldDim;
        } else if (xaeroPlus$worldChange_prevMapWorldUsable && mapWorldUsable && xaeroPlus$worldChange_prevMapWorldCurrentDimId != mapWorldDim) {
            type = VIEWED_DIMENSION_SWITCH;
            from = xaeroPlus$worldChange_prevMapWorldCurrentDimId;
            to = mapWorldDim;
        } else if (!Objects.equals(xaeroPlus$worldChange_prevCurrentMWId, currentMWId) && xaeroPlus$worldChange_prevWorld.get() == world) {
            type = MULTIWORLD_SWITCH;
            from = null;
            to = null;
        } else {
            XaeroPlus.LOGGER.warn("Unhandled XaeroWorldChangeEvent type :(");
            return;
        }
        XaeroPlus.LOGGER.info("Firing world change event: {} from {} to {}", type, from, to);
        var event = new XaeroWorldChangeEvent(type, from, to);
        XaeroPlus.EVENT_BUS.call(event);
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
