package xaeroplus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import xaero.common.HudMod;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.map.WorldMap;
import xaero.map.core.XaeroWorldMapCore;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.feature.waypoint.WaypointAPI;
import xaeroplus.module.ModuleManager;
import xaeroplus.module.impl.Drawing;
import xaeroplus.module.impl.Pearls;
import xaeroplus.module.impl.SpawnPoint;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.settings.Settings;
import xaeroplus.util.AtlasWaypointImport;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.normalizer.LossReport;
import xaeroplus.util.normalizer.RegionNormalizer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class XPCommandManager {
    private XPCommandManager() {}

    static LiteralArgumentBuilder<XPClientCommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    static <T> RequiredArgumentBuilder<XPClientCommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    public static void registerCommands(CommandDispatcher<XPClientCommandSource> dispatcher, CommandBuildContext context) {
        dispatcher.register(literal("xaeroDataDir").executes(c -> {
            c.getSource().xaeroplus$sendSuccess(DataFolderResolveUtil.getCurrentDataDirPath());
            return 1;
        }));
        dispatcher.register(literal("xaeroWaypointDir").executes(c -> {
            c.getSource().xaeroplus$sendSuccess(DataFolderResolveUtil.getCurrentWaypointDataDirPath());
            return 1;
        }));
        dispatcher.register(literal("xaero2b2tAtlasImport").executes(c -> {
            c.getSource().xaeroplus$sendSuccess(Component.literal("Atlas import started..."));
            AtlasWaypointImport.importAtlasWaypoints()
                .whenCompleteAsync((addedCount, e) -> {
                    if (e != null) {
                        XaeroPlus.LOGGER.error("Atlas import failed", e);
                        c.getSource().xaeroplus$sendFailure(Component.literal("Atlas import failed! Check log for details."));
                    } else {
                        c.getSource().xaeroplus$sendSuccess(Component.literal(addedCount + " waypoints imported to the \"atlas\" waypoint set!"));
                        boolean allSetsEnabled = HudMod.INSTANCE.getHudConfigs().getClientConfigManager().getEffective(
                            MinimapProfiledConfigOptions.WAYPOINTS_ALL_SETS);
                        boolean isAtlasSetActive = Optional.ofNullable(WaypointAPI.getCurrentWaypointSet())
                            .map(WaypointSet::getName)
                            .filter(n -> n.equals("atlas"))
                            .isPresent();
                        if (!allSetsEnabled && !isAtlasSetActive) {
                            c.getSource().xaeroplus$sendSuccess(Component.literal("To see the waypoints, enable rendering all waypoint sets or switch to the \"atlas\" set."));
                        }
                    }
                    c.getSource().xaeroplus$sendSuccess(Component.literal("Atlas Import Complete!"));
                }, TickTaskExecutor.INSTANCE);
            return 1;
        }));
        dispatcher.register(literal("xaeroplus:clearDrawings").executes(c -> {
            TickTaskExecutor.INSTANCE.submit(() -> {
                ModuleManager.getModule(Drawing.class).clearAll();
                c.getSource().xaeroplus$sendSuccess(Component.literal("All Drawings cleared!"));
            });
            return 1;
        }));
        dispatcher.register(literal("xaeroplus:resetDrawOrder").executes(c -> {
            TickTaskExecutor.INSTANCE.submit(() -> {
                Settings.REGISTRY.drawOrderSetting.setValue("");
                c.getSource().xaeroplus$sendSuccess(Component.literal("Draw order reset!"));
            });
            return 1;
        }));
        dispatcher.register(literal("xaeroplus:clearSpawnPoints").executes(c -> {
            TickTaskExecutor.INSTANCE.submit(() -> {
                ModuleManager.getModule(SpawnPoint.class).getLoadedSpawnPositions().clear();
                ModuleManager.getModule(SpawnPoint.class).saveRespawnPoints();
                c.getSource().xaeroplus$sendSuccess(Component.literal("All spawn points cleared!"));
            });
            return 1;
        }));
        dispatcher.register(literal("xaeroplus:clearPearls").executes(c -> {
            TickTaskExecutor.INSTANCE.submit(() -> {
                ModuleManager.getModule(Pearls.class).getLoadedPearls().clear();
                ModuleManager.getModule(Pearls.class).savePearls();
                c.getSource().xaeroplus$sendSuccess(Component.literal("All pearls cleared!"));
            });
            return 1;
        }));

        // /xaeroplus:convert <targetMajor> <targetMinor>
        // Converts all region files in the current world's data folder in-place
        // to the specified Xaero format version. Supports hot-reload.
        dispatcher.register(literal("xaeroplus:convert")
            .then(argument("targetMajor", IntegerArgumentType.integer(0, 7))
                .then(argument("targetMinor", IntegerArgumentType.integer(0, 8))
                    .executes(c -> {
                        int targetMajor = IntegerArgumentType.getInteger(c, "targetMajor");
                        int targetMinor = IntegerArgumentType.getInteger(c, "targetMinor");
                        return executeConvert(c.getSource(), targetMajor, targetMinor);
                    })
                )
            )
        );

        // /xaeroplus:convert info — show current data dir info
        dispatcher.register(literal("xaeroplus:convert")
            .then(literal("info").executes(c -> {
                var session = XaeroWorldMapCore.currentSession;
                if (session == null) {
                    c.getSource().xaeroplus$sendFailure(Component.literal("No active world map session."));
                    return 0;
                }
                var mp = session.getMapProcessor();
                String worldId = mp.getCurrentWorldId();
                if (worldId == null || WorldMap.saveFolder == null) {
                    c.getSource().xaeroplus$sendFailure(Component.literal("No world data loaded."));
                    return 0;
                }
                Path dataDir = WorldMap.saveFolder.toPath().resolve(worldId);
                c.getSource().xaeroplus$sendSuccess(Component.literal(
                    "World data: " + dataDir + "\nCurrent format: major 7, minor 8 (latest)"));
                return 1;
            }))
        );
    }

    private static int executeConvert(XPClientCommandSource source, int targetMajor, int targetMinor) {
        var session = XaeroWorldMapCore.currentSession;
        if (session == null) {
            source.xaeroplus$sendFailure(Component.literal("No active world map session."));
            return 0;
        }

        var mapProcessor = session.getMapProcessor();
        String worldId = mapProcessor.getCurrentWorldId();
        if (worldId == null || WorldMap.saveFolder == null) {
            source.xaeroplus$sendFailure(Component.literal("No world data loaded."));
            return 0;
        }

        Path saveFolder = WorldMap.saveFolder.toPath();
        Path worldFolder = saveFolder.resolve(worldId);

        source.xaeroplus$sendSuccess(Component.literal(
            "Starting conversion to format " + targetMajor + "." + targetMinor + "..."));
        source.xaeroplus$sendSuccess(Component.literal(
            "Data folder: " + worldFolder));

        // Run conversion asynchronously to avoid blocking the main thread
        CompletableFuture.supplyAsync(() -> {
            try {
                RegionNormalizer normalizer = new RegionNormalizer();
                // In-place conversion with atomic writes and hot-reload signals
                LossReport report = normalizer.convertDirectory(
                    worldFolder, worldFolder,
                    targetMajor, targetMinor, true, // is115flag = true (1.15+ assumed for in-game use)
                    true,  // invalidateCaches
                    true,  // atomic writes
                    worldFolder  // signal folder for hot-reload
                );
                return report;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Globals.cacheRefreshExecutorService.get()).whenCompleteAsync((report, error) -> {
            if (error != null) {
                XaeroPlus.LOGGER.error("Region conversion failed", error);
                source.xaeroplus$sendFailure(Component.literal(
                    "Conversion failed: " + error.getMessage()));
            } else {
                int warnings = report.count(LossReport.Severity.WARNING);
                int errors = report.count(LossReport.Severity.ERROR);

                // Show summary
                for (LossReport.Entry entry : report.entries()) {
                    if (entry.category().equals("summary")) {
                        source.xaeroplus$sendSuccess(Component.literal(entry.message()));
                    }
                }

                if (errors > 0) {
                    source.xaeroplus$sendFailure(Component.literal(
                        errors + " errors occurred. Check log for details."));
                }
                if (warnings > 0) {
                    source.xaeroplus$sendSuccess(Component.literal(
                        warnings + " lossy operations (expected for backporting)."));
                }

                source.xaeroplus$sendSuccess(Component.literal(
                    "Conversion complete! Map should reload automatically."));

                // Log all losses
                if (report.hasLosses()) {
                    XaeroPlus.LOGGER.info("[Normalizer] Loss report:\n{}", report.toString());
                }
            }
        }, TickTaskExecutor.INSTANCE);

        return 1;
    }
}
