package xaeroplus.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.Component;
import xaero.common.HudMod;
import xaero.hud.minimap.common.config.option.MinimapProfiledConfigOptions;
import xaero.hud.minimap.waypoint.set.WaypointSet;
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

import java.util.Optional;

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
    }
}
