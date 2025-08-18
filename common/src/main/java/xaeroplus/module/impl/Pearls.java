package xaeroplus.module.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.extensions.SyncedWaypoint;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.Module;
import xaeroplus.util.FileUtil;
import xaeroplus.util.timer.Timer;
import xaeroplus.util.timer.Timers;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import static net.minecraft.world.level.Level.*;

public class Pearls extends Module {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<UUID, Map<UUID, Pearl>> pearlsMap = new ConcurrentHashMap<>();
    private boolean stale = false;
    public static final String PEARL_WP_SET_ID = "xaeroplus.gui.pearl_waypoints_set";
    private final Timer updateTracksPearlsTimer = Timers.tickTimer();
    private final Timer trackedPearlRemovalTimer = Timers.tickTimer();

    @Override
    public void onEnable() {
        this.pearlsMap.clear();
        loadPearls();
    }

    @Override
    public void onDisable() {
        savePearls();
        deleteAllPearlWaypointSets();
        this.pearlsMap.clear();
    }

    @EventHandler
    public void updateTrackedPearls(ClientTickEvent.Post event) {
        if (!updateTracksPearlsTimer.tick(10)) return;
        var level = mc.level;
        if (level == null) return;
        var con = mc.getConnection();
        if (con == null) return;
        var selfUuid = con.getLocalGameProfile().getId();
        if (selfUuid == null) return;
        boolean updated = false;
        for (var entity : level.entitiesForRendering()) {
            if (!(entity instanceof ThrownEnderpearl thrownPearlEntity)) continue;
            var pearlUUID = thrownPearlEntity.getUUID();
            var pearlOwner = thrownPearlEntity.getOwner();
            // if the owner, including ourselves, is currently in the world, they will be set as the owner
            // otherwise, we don't know who the owner is
            if (pearlOwner == null) continue;
            // todo: think about whether we should track pearls thrown by other players
            if (!pearlOwner.getUUID().equals(selfUuid)) continue;

            if (thrownPearlEntity.tickCount < 20) continue;

            // we only want to track pearls in some form of stasis chamber
            if (Math.abs(thrownPearlEntity.getDeltaMovement().x()) > 0.01
                || Math.abs(thrownPearlEntity.getDeltaMovement().y()) > 1.0
                || Math.abs(thrownPearlEntity.getDeltaMovement().z()) > 0.01) {
                continue;
            }

            var pearls = pearlsMap.computeIfAbsent(pearlOwner.getUUID(), e -> new ConcurrentHashMap<>());
            var existingPearl = pearls.get(pearlUUID);
            if (existingPearl == null || thrownPearlEntity.distanceToSqr(existingPearl.x(), existingPearl.y(), existingPearl.z()) > 1.0) {
                pearls.put(pearlUUID, new Pearl(
                    pearlUUID,
                    level.dimension().location().toString(),
                    thrownPearlEntity.blockPosition().getX(),
                    thrownPearlEntity.blockPosition().getY(),
                    thrownPearlEntity.blockPosition().getZ()
                ));
                updated = true;
            }
        }
        if (updated) {
            savePearlsAsync();
        }
    }

    @EventHandler
    public void trackedPearlRemoval(ClientTickEvent.Post event) {
        if (!trackedPearlRemovalTimer.tick(11)) return;
        var level = mc.level;
        if (level == null) return;
        var con = mc.getConnection();
        if (con == null) return;
        var selfUuid = con.getLocalGameProfile().getId();
        if (selfUuid == null) return;
        var player = mc.player;
        if (player == null) return;

        var effectiveRenderDistance = mc.options.getEffectiveRenderDistance();
        var trackedEntityRadius = Math.min(3, effectiveRenderDistance + 1) * 16;
        var trackedEntityRadiusSq = trackedEntityRadius * trackedEntityRadius;

        var savedPearls = pearlsMap.getOrDefault(selfUuid, Collections.emptyMap());
        boolean updated = false;
        for (var it = savedPearls.values().iterator(); it.hasNext(); ) {
            final var savedPearl = it.next();
            if (savedPearl.dimension() != level.dimension()) continue;
            if (player.distanceToSqr(savedPearl.x(), player.getY(), savedPearl.z()) <= trackedEntityRadiusSq) {
                if (level.entityStorage.getEntityGetter().get(savedPearl.uuid) == null) {
                    it.remove();
                    updated = true;
                }
            }
        }
        if (updated) {
            savePearlsAsync();
        }
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        switch (event.worldChangeType()) {
            case EXIT_WORLD -> {
                savePearls();
                this.pearlsMap.clear();
            }
            case ENTER_WORLD -> {
                this.pearlsMap.clear();
                loadPearls();
            }
        }
    }

    @EventHandler
    public void syncPearlsToWaypoints(ClientTickEvent.Post event) {
        if (!stale) return;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return;
        var con = mc.getConnection();
        if (con == null) return;
        var selfUuid = con.getLocalGameProfile().getId();
        if (selfUuid == null) return;

        clearAllPearlWaypoints();

        var pearls = pearlsMap.get(selfUuid);
        if (pearls == null) return;
        for (var pearl : pearls.values()) {
            var pearlDim = pearl.dimension();
            var minimapWorld = WaypointAPI.getMinimapWorld(pearlDim);
            if (minimapWorld == null) return;
            var wpSet = WaypointAPI.getOrCreateWaypointSetInWorld(minimapWorld, PEARL_WP_SET_ID);
            var wp = SyncedWaypoint.create(
                pearl.x(),
                pearl.y(),
                pearl.z(),
                "Pearl",
                "P",
                getPearlWaypointColor(pearl)
            );
            wpSet.add(wp);
        }
        stale = false;
    }

    private WaypointColor getPearlWaypointColor(Pearl pearl) {
        int index = Math.abs(
            pearl.uuid().hashCode() % WaypointColor.values().length
        );
        return WaypointColor.fromIndex(index);
    }

    private void clearAllPearlWaypoints() {
        WaypointAPI.forEachWaypointSetInAllMinimapWorlds(wpSet -> {
            if (PEARL_WP_SET_ID.equals(wpSet.getName())) {
                wpSet.clear();
            }
        });
    }

    private void deleteAllPearlWaypointSets() {
        WaypointAPI.forEachMinimapWorld(world -> {
            var currentWpSet = world.getCurrentWaypointSetId();
            if (currentWpSet != null && currentWpSet.equals(PEARL_WP_SET_ID)) {
                world.setCurrentWaypointSetId("gui.xaero_default");
            }
            world.removeWaypointSet(PEARL_WP_SET_ID);
        });
    }

    private File getSaveFile() {
        var currentSession = XaeroWorldMapCore.currentSession;
        if (currentSession == null) return null;
        MapProcessor mapProcessor = currentSession.getMapProcessor();
        if (mapProcessor == null) return null;
        final String worldId = mapProcessor.getCurrentWorldId();
        if (worldId == null) return null;
        if (WorldMap.saveFolder == null) return null;
        return WorldMap.saveFolder.toPath()
            .resolve(worldId)
            .resolve("xaeroplus-pearls.json")
            .toFile();
    }

    public synchronized void loadPearls() {
        try {
            var saveFile = getSaveFile();
            if (saveFile == null) {
                return;
            }
            if (!saveFile.exists()) {
                return;
            }
            try (var reader = Files.newBufferedReader(saveFile.toPath())) {
                Map<UUID, Map<UUID, Pearl>> map = gson.fromJson(reader, new TypeToken<Map<UUID, Map<UUID, Pearl>>>() {}.getType());
                if (map != null) {
                    this.pearlsMap.clear();
                    this.pearlsMap.putAll(map);
                }
                stale = true;
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("[Pearls] Failed to read pearls file", e);
        }
    }

    public void savePearlsAsync() {
        stale = true;
        ForkJoinPool.commonPool().execute(this::savePearls);
    }

    public synchronized void savePearls() {
        try {
            var saveFile = getSaveFile();
            if (saveFile == null) return;
            FileUtil.safeSave(saveFile, writer -> {
                gson.toJson(pearlsMap, new TypeToken<Map<UUID, Map<UUID, Pearl>>>() {}.getType(), writer);
            });
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("[Pearls] Failed to write pearls file", e);
        }
    }

    public Map<UUID, Map<UUID, Pearl>> getLoadedPearls() {
        return pearlsMap;
    }

    public record Pearl(
        UUID uuid,
        String dimensionKey,
        int x,
        int y,
        int z
    ) {
        public ResourceLocation dimensionLocation() {
            return ResourceLocation.tryParse(dimensionKey);
        }

        public ResourceKey<Level> dimension() {
            var dimensionLocation = dimensionLocation();
            if (dimensionLocation == null) return null;
            var ow = OVERWORLD.location();
            var nether = NETHER.location();
            var end = END.location();
            if (dimensionLocation.equals(ow)) {
                return OVERWORLD;
            } else if (dimensionLocation.equals(nether)) {
                return NETHER;
            } else if (dimensionLocation.equals(end)) {
                return END;
            }
            var level = Minecraft.getInstance().level;
            if (level != null) {
                if (level.dimension().location().equals(dimensionLocation)) {
                    return level.dimension();
                }
            }
            return ResourceKey.create(Registries.DIMENSION, dimensionLocation);
        }
    }
}
