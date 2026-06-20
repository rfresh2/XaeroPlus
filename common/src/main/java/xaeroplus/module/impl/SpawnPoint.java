package xaeroplus.module.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
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
    private boolean wasDead = false;
    private int respawnCheckDelay = 0;
    private long respawnPointSetTimestamp = 0;

    @Override
    public void onEnable() {
        if (mc.level == null) return;
        this.wasDead = false;
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
                event.pos().getX(),
                event.pos().getY(),
                event.pos().getZ()));
                respawnPointSetTimestamp = System.currentTimeMillis() + 3000;
        saveRespawnPointsAsync();
    }

    @EventHandler
    public void onXaeroWorldChange(XaeroWorldChangeEvent event) {
        switch (event.worldChangeType()) {
            case EXIT_WORLD -> {
                this.respawnPointSetTimestamp = 0;
                this.wasDead = false;
                saveRespawnPoints();
                clearWpAndState();
                this.respawnPoints.clear();
            }
            case ENTER_WORLD -> {
                this.respawnPointSetTimestamp = 0;
                this.wasDead = false;
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
        if (minimapSession.getWorldManager().getCurrentWorld() == null) return;
        var con = mc.getConnection();
        if (con == null) return;
        var uuid = con.getLocalGameProfile().getId();
        var spawnPoint = respawnPoints.get(uuid);
        if (spawnPoint == null) {
            clearWpAndState();
            return;
        }

        var player = mc.player;
        if (player == null) return;
        boolean isAlive = !player.isDeadOrDying();

        if (isAlive) {
            if (checkAndRemoveIfInvalid(uuid, spawnPoint)) return;
            if (wasDead) respawnCheckDelay = 5;
        }

        wasDead = !isAlive;
        if (respawnCheckDelay > 0) {
            respawnCheckDelay--;
            if (respawnCheckDelay == 0) {
                if (isAlive && respawnPoints.containsKey(uuid)) {
                    if (!isValidSpawnPosition(player.blockPosition(), spawnPoint)) {
                        removeSpawnPoint(uuid);
                        XaeroPlus.LOGGER.info("[SpawnPoint] Respawn obstructed. Waypoint removed");
                        return;
                    }
                }
            }
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
                spawnPoint.x() * 8, spawnPoint.y(), spawnPoint.z() * 8);
        }
        var minimapWorld = WaypointAPI.getMinimapWorld(spawnPointDimension);
        if (minimapWorld == null) {
            clearWpAndState();
            return;
        }
        WaypointSet waypointSet = WaypointAPI.getOrCreateWaypointSetInWorld(minimapWorld, "gui.xaero_default");

        if (!isWaypointStateValid(spawnPoint)) {
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
        removeWaypointFromSet();
        wpRef = nullRef;
        wpSetRef = nullRef;
        wpSpawnPoint = null;
    }

    private void removeSpawnPoint(UUID uuid) {
        respawnPoints.remove(uuid);
        clearWpAndState();
        saveRespawnPointsAsync();
    }

    private boolean isValidSpawnPosition(BlockPos playerPos, SpawnPosition spawnPoint) {
        int px = playerPos.getX(), py = playerPos.getY(), pz = playerPos.getZ();
        int cx = spawnPoint.x(), cy = spawnPoint.y(), cz = spawnPoint.z();
        if (spawnPoint.dimension() == NETHER)
            return Math.abs(px - cx) <= 1 && (py == cy || py == cy + 1) && Math.abs(pz - cz) <= 1;

        var level = mc.player.clientLevel;
        BlockPos headPos = new BlockPos(cx, cy, cz);
        if (level.isLoaded(headPos)) {
            var headState = level.getBlockState(headPos);
            if (headState.is(BlockTags.BEDS)) {
                var facing = headState.getValue(net.minecraft.world.level.block.BedBlock.FACING);
                BlockPos footPos = headPos.relative(facing.getOpposite());
                if (isAdjacentToBed(playerPos, headPos) || isAdjacentToBed(playerPos, footPos)) {
                    return true;
                }
            }
        }

        return px == cx && py == cy && pz == cz
                || px == cx + 1 && py == cy && pz == cz
                || px == cx - 1 && py == cy && pz == cz
                || px == cx && py == cy && pz == cz + 1
                || px == cx && py == cy && pz == cz - 1;
    }

    private boolean isAdjacentToBed(BlockPos playerPos, BlockPos bedPos) {
        if (playerPos.getY() != bedPos.getY()) return false;
        int dx = playerPos.getX() - bedPos.getX();
        int dz = playerPos.getZ() - bedPos.getZ();
        return (Math.abs(dx) == 1 && dz == 0) || (Math.abs(dz) == 1 && dx == 0) || playerPos.equals(bedPos);
    }

    private boolean checkAndRemoveIfInvalid(UUID uuid, SpawnPosition spawnPoint) {
        if (System.currentTimeMillis() < respawnPointSetTimestamp) return false;
        var player = mc.player;
        if (player == null) return false;
        var level = player.clientLevel;
        ResourceKey<Level> currentDim = level.dimension();
        if (!currentDim.equals(spawnPoint.dimension())) return false;
        BlockPos center = new BlockPos(spawnPoint.x(), spawnPoint.y(), spawnPoint.z());
        if (!level.isLoaded(center)) return false;
        var state = level.getBlockState(center);
        boolean isValid;
        if (spawnPoint.dimension() == NETHER) {
            isValid = state.is(Blocks.RESPAWN_ANCHOR) && state.getValue(RespawnAnchorBlock.CHARGE) > 0;
        } else {
            isValid = state.is(BlockTags.BEDS);
        }
        if (!isValid) {
            removeSpawnPoint(uuid);
            XaeroPlus.LOGGER.info("[SpawnPoint] Bed/Anchor destroyed. Waypoint removed");
            return true;
        }
        return false;
    }

    private synchronized void removeWaypointFromSet() {
        WaypointSet set = wpSetRef.get();
        Waypoint wp = wpRef.get();
        if (set != null && wp != null) set.remove(wp);
    }

    private boolean isWaypointStateValid(SpawnPosition spawnPoint) {
        Waypoint wp = wpRef.get();
        WaypointSet set = wpSetRef.get();
        boolean spawnUnchanged = Objects.equals(wpSpawnPoint, spawnPoint);
        boolean stateExists = wp != null && set != null;
        return spawnUnchanged && stateExists;
    }

    private File getSaveFile() {
        var currentSession = XaeroWorldMapCore.currentSession;
        if (currentSession == null) return null;
        MapProcessor mapProcessor = currentSession.getMapProcessor();
        if (mapProcessor == null) return null;
        final String worldId = mapProcessor.getCurrentWorldId();
        if (worldId == null) return null;
        if (WorldMap.saveFolder == null) return null;
        return WorldMap.saveFolder.toPath().resolve(worldId).resolve("xaeroplus-respawn-points.json").toFile();
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
        private static final Map<String, ResourceKey<Level>> DIMENSION_CACHE = new ConcurrentHashMap<>();

        public ResourceKey<Level> dimension() {
            ResourceKey<Level> cached = DIMENSION_CACHE.get(dimensionKey);
            if (cached != null) return cached;
            ResourceKey<Level> parsed = parseDimension(dimensionKey);
            if (parsed != null) DIMENSION_CACHE.put(dimensionKey, parsed);
            return parsed;
        }

        public static ResourceKey<Level> parseDimension(String key) {
            if (key == null) return null;
            ResourceLocation location = ResourceLocation.tryParse(key);
            if (location == null) return null;
            if (location.equals(OVERWORLD.location())) return OVERWORLD;
            if (location.equals(NETHER.location())) return NETHER;
            if (location.equals(END.location())) return END;
            return ResourceKey.create(Registries.DIMENSION, location);
        }
    }
}