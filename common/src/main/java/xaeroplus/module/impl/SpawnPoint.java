package xaeroplus.module.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.MapProcessor;
import xaero.map.WorldMap;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.RespawnPointSetEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.extensions.SyncedWaypoint;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.FileUtil;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

import static net.minecraft.world.level.Level.*;

public class SpawnPoint extends Module {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Map<UUID, SpawnPosition> respawnPoints = new ConcurrentHashMap<>();
    private static final WeakReference nullRef = new WeakReference<>(null);
    private WeakReference<Waypoint> wpRef = nullRef;
    private WeakReference<WaypointSet> wpSetRef = nullRef;
    private SpawnPosition wpSpawnPoint = null;

    @Override
    public void onEnable() {
        if (mc.level == null) return;
        this.respawnPoints.clear();
        loadRespawnPoints();
    }

    @Override
    public void onDisable() {
        if (mc.level == null) return;
        saveRespawnPoints();
        this.respawnPoints.clear();
        clearWpAndState();
    }

    @EventHandler
    public void onRespawnPointSet(RespawnPointSetEvent event) {
        var con = mc.getConnection();
        if (con == null) return;
        UUID activeUUID = con.getLocalGameProfile().getId();
        respawnPoints.put(activeUUID, new SpawnPosition(
            ChunkUtils.getActualDimension().location().toString(),
            event.pos().getX(), event.pos().getY(), event.pos().getZ()
        ));
        saveRespawnPointsAsync();
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        switch (event.worldChangeType()) {
            case EXIT_WORLD -> {
                saveRespawnPoints();
                clearWpAndState();
                this.respawnPoints.clear();
            }
            case ENTER_WORLD -> {
                clearWpAndState();
                this.respawnPoints.clear();
                loadRespawnPoints();
            }
        }
    }

    @EventHandler
    public void onClientTick(ClientTickEvent.Post event) {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return;
        var con = mc.getConnection();
        if (con == null) return;
        var uuid = con.getLocalGameProfile().getId();
        var spawnPoint = respawnPoints.get(uuid);
        if (spawnPoint == null) {
            clearWpAndState();
            return;
        }


        var spawnPointDimension = spawnPoint.dimension();
        if (spawnPointDimension == null) {
            clearWpAndState();
            return;
        }
        if (Settings.REGISTRY.owAutoWaypointDimension.get() && spawnPointDimension == NETHER) {
            spawnPointDimension = OVERWORLD;
            spawnPoint = new SpawnPosition(
                OVERWORLD.location().toString(),
                spawnPoint.x() * 8, spawnPoint.y(), spawnPoint.z() * 8
            );
        }
        var minimapWorld = WaypointAPI.getMinimapWorld(spawnPointDimension);
        if (minimapWorld == null) {
            clearWpAndState();
            return;
        }
        WaypointSet waypointSet = WaypointAPI.getOrCreateWaypointSetInWorld(minimapWorld, "gui.xaero_default");

        if (!Objects.equals(wpSpawnPoint, spawnPoint) || wpSetRef.get() == null || wpRef.get() == null) {
            clearWpAndState();
            wpSetRef = new WeakReference<>(waypointSet);
            Waypoint wp = SyncedWaypoint.create(
                spawnPoint.x(),
                spawnPoint.y(),
                spawnPoint.z(),
                "Spawn Point",
                "SP",
                WaypointColor.AQUA
            );
            waypointSet.add(wp);
            wpRef = new WeakReference<>(wp);
            wpSpawnPoint = spawnPoint;
            XaeroPlus.LOGGER.info("[SpawnPoint] Spawn Point Waypoint Updated: {} {} {}", spawnPoint.x(), spawnPoint.y(), spawnPoint.z());
        }
    }

    private synchronized void clearWpAndState() {
        if (wpRef.get() != null && wpSetRef.get() != null) wpSetRef.get().remove(this.wpRef.get());
        wpRef = nullRef;
        wpSetRef = nullRef;
        wpSpawnPoint = null;
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
            .resolve("xaeroplus-respawn-points.json")
            .toFile();
    }

    public synchronized void loadRespawnPoints() {
        try {
            var saveFile = getSaveFile();
            if (saveFile == null) {
                return;
            }
            if (!saveFile.exists()) {
                return;
            }
            try (var reader = Files.newBufferedReader(saveFile.toPath())) {
                Map<UUID, SpawnPosition> map = gson.fromJson(reader, new TypeToken<Map<UUID, SpawnPosition>>() {}.getType());
                if (map != null) {
                    this.respawnPoints.clear();
                    this.respawnPoints.putAll(map);
                }
            }
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("[SpawnPoint] Failed to read respawn points file", e);
        }
    }

    public void saveRespawnPointsAsync() {
        ForkJoinPool.commonPool().execute(this::saveRespawnPoints);
    }

    public synchronized void saveRespawnPoints() {
        try {
            var saveFile = getSaveFile();
            if (saveFile == null) return;
            FileUtil.safeSave(saveFile, writer -> {
                gson.toJson(respawnPoints, new TypeToken<Map<UUID, SpawnPosition>>() {}.getType(), writer);
            });
        } catch (Exception e) {
            XaeroPlus.LOGGER.error("[SpawnPoint] Failed to write respawn points file", e);
        }
    }

    public Map<UUID, SpawnPosition> getLoadedSpawnPositions() {
        return respawnPoints;
    }

    public record SpawnPosition(String dimensionKey, int x, int y, int z) {

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
