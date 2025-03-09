package xaeroplus.util;

import com.google.common.hash.Hashing;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.map.mods.SupportMods;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.TickTaskExecutor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public final class AtlasWaypointImport {
    private AtlasWaypointImport() {}

    public static CompletableFuture<Integer> importAtlasWaypoints() {
        return CompletableFuture
            .supplyAsync(AtlasWaypointImport::getAtlasApiResponse, ForkJoinPool.commonPool())
            .thenApplyAsync((atlasWaypoints) -> {
                int addedWaypoints = importAtlasWaypoints(atlasWaypoints);
                XaeroPlus.LOGGER.info("Imported {} Atlas waypoints", addedWaypoints);
                return addedWaypoints;
            }, ModuleManager.getModule(TickTaskExecutor.class));
    }

    private static int importAtlasWaypoints(List<AtlasWaypoint> atlasWaypoints) {
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return 0;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return 0;
        WaypointSet currentWpSet = currentWorld.getCurrentWaypointSet();
        if (currentWpSet == null) return 0;
        if (atlasWaypoints.isEmpty()) return 0;
        Map<ResourceKey<Level>, ArrayList<AtlasWaypoint>> atlasByDimension = atlasWaypoints.stream()
            .filter(AtlasWaypoint::isValid)
            .collect(Collectors.toMap(
                k -> Objects.equals(0, k.end_dimension)
                    ? Level.OVERWORLD
                    : Level.END,
                v -> {
                    var l = new ArrayList<AtlasWaypoint>();
                    l.add(v);
                    return l;
                },
                (v1, v2) -> {
                    v1.addAll(v2);
                    return v1;
                }
            ));
        MinimapWorld owMinimapWorld = WaypointAPI.getMinimapWorld(Level.OVERWORLD);
        MinimapWorld endMinimapWorld = WaypointAPI.getMinimapWorld(Level.END);
        WaypointSet owAtlasSet = WaypointAPI.getOrCreateWaypointSetInWorld(owMinimapWorld, "atlas");
        WaypointSet endAtlasSet = WaypointAPI.getOrCreateWaypointSetInWorld(endMinimapWorld, "atlas");
        owAtlasSet.clear();
        endAtlasSet.clear();

        int addedWaypoints = 0;
        for (var atlasWp : atlasByDimension.entrySet()) {
            ResourceKey<Level> dim = atlasWp.getKey();
            List<AtlasWaypoint> waypoints = atlasWp.getValue();
            WaypointSet waypointSet = dim == Level.OVERWORLD ? owAtlasSet : endAtlasSet;
            for (var waypoint : waypoints) {
                int index = Math.abs(
                    Hashing.murmur3_128().hashUnencodedChars(waypoint.name).asInt())
                    % WaypointColor.values().length;
                var color = WaypointColor.fromIndex(index);
                Waypoint wp = new Waypoint(
                    waypoint.x,
                    waypoint.y == null ? 64 : waypoint.y,
                    waypoint.z,
                    waypoint.name,
                    waypoint.name.substring(0, Math.min(2, waypoint.name.length())),
                    color,
                    WaypointPurpose.NORMAL
                );
                waypointSet.add(wp);
                addedWaypoints++;
            }
        }
        SupportMods.xaeroMinimap.requestWaypointsRefresh();
        return addedWaypoints;
    }

    private static List<AtlasWaypoint> getAtlasApiResponse() {
        HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        try {
            HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("https://2b2tAtlas.com/api/locations.php"))
                .headers("User-Agent", "XaeroPlus/" + XaeroPlus.XP_VERSION + "+" + XaeroPlus.MC_VERSION)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            var response = client.send(req, HttpResponse.BodyHandlers.ofString());
            var gson = new GsonBuilder()
                .disableHtmlEscaping()
                .setLenient()
                .create();
            return gson.fromJson(response.body(), new TypeToken<List<AtlasWaypoint>>() {});
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Failed to get Atlas API response", e);
            return Collections.emptyList();
        }
    }

    public static class AtlasWaypoint {
        public String name;
        public Integer x;
        @Nullable public Integer y;
        public Integer z;
        public Integer end_dimension;

        public boolean isValid() {
            return name != null && x != null && z != null && end_dimension != null;
        }
    }
}
