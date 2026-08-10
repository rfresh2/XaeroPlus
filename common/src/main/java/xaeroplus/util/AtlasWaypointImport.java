package xaeroplus.util;

import com.google.common.hash.Hashing;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.map.mods.SupportMods;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.impl.TickTaskExecutor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
            }, TickTaskExecutor.INSTANCE);
    }

    public static int importAtlasWaypoints(List<AtlasWaypoint> atlasWaypoints) {
        var minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return 0;
        var currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return 0;
        var currentWpSet = currentWorld.getCurrentWaypointSet();
        if (currentWpSet == null) return 0;
        if (atlasWaypoints.isEmpty()) return 0;
        Map<ResourceKey<Level>, ArrayList<AtlasWaypoint>> atlasByDimension = atlasWaypoints.stream()
            .filter(AtlasWaypoint::isValid)
            .collect(Collectors.toMap(
                k -> k.dimension == 0
                    ? Level.OVERWORLD
                    : k.dimension == 1
                        ? Level.NETHER
                        : k.dimension == 2
                            ? Level.END
                            : null,
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

        int addedWaypoints = 0;
        for (var dim : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            var minimapWorld = WaypointAPI.getMinimapWorld(dim);
            var waypointSet = WaypointAPI.getOrCreateWaypointSetInWorld(minimapWorld, "atlas");
            waypointSet.clear();
            var waypoints = atlasByDimension.get(dim);
            if (waypoints == null) continue;
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

    public static List<AtlasWaypoint> getAtlasApiResponse() {
        var client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
        try {
            var req = HttpRequest.newBuilder()
                // docs: https://2b2tatlas.com/api
                .uri(URI.create("https://api.blackportal.cloud/api/locations"))
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

    public record AtlasWaypoint(
        String name,
        Integer x,
        Integer y, // nullable
        Integer z,
        Integer dimension
    ) {
        public boolean isValid() {
            return name != null && x != null && z != null && dimension != null && dimension >= 0 && dimension <= 2;
        }
    }
}
