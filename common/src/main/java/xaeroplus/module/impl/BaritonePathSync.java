package xaeroplus.module.impl;

import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.world.MinimapWorld;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.feature.render.DrawFeatureFactory;
import xaeroplus.feature.render.line.Line;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.BaritoneHelper;
import xaeroplus.util.BaritonePathHelper;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BaritonePathSync extends Module {
    private int color = ColorHelper.getColor(255, 0, 0, 150);
    private List<Line> pathLines = new ArrayList<>();
    private float lineWidth = 0.1f; // todo: setting?

    @Override
    public void onEnable() {
        Globals.drawManager.registry().register(
            DrawFeatureFactory.lines(
                "BaritonePath",
                this::getPathLines,
                this::getColor,
                this::getLineWidth,
                50
            )
        );
    }

    @Override
    public void onDisable() {
        Globals.drawManager.registry().unregister("BaritonePath");
    }

    @EventHandler
    public void syncPath(final ClientTickEvent.Post event) {
        if (!BaritoneHelper.isBaritonePresent()) return;
        MinimapSession minimapSession = BuiltInHudModules.MINIMAP.getCurrentSession();
        if (minimapSession == null) return;
        MinimapWorld currentWorld = minimapSession.getWorldManager().getCurrentWorld();
        if (currentWorld == null) return;
        try {
            var baritonePath = BaritonePathHelper.getBaritonePath();
            if (baritonePath.isEmpty()) {
                pathLines = Collections.emptyList();
                return;
            }
            List<Line> lines = new ArrayList<>();
            var prevPos = baritonePath.get(0);
            for (int i = 1; i < baritonePath.size(); i++) {
                var currentPos = baritonePath.get(i);
                if (currentPos.getX() == prevPos.getX() && currentPos.getZ() == prevPos.getZ())
                    continue;

                var line = new Line(prevPos.getX(), prevPos.getZ(), currentPos.getX(), currentPos.getZ());
                lines.add(line);
                prevPos = currentPos;
            }
            pathLines = lines;
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error in Baritone path sync", e);
        }
    }

    public int getColor() {
        return color;
    }

    public void setColor(final int color) {
        this.color = ColorHelper.getColorWithAlpha(color, Settings.REGISTRY.baritonePathSyncOpacity.getAsInt());
    }

    public void setOpacity(final int opacity) {
        this.color = ColorHelper.getColorWithAlpha(color, opacity);
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public List<Line> getPathLines(int windowRegionX, int windowRegionZ, int windowSize, ResourceKey<Level> dimension) {
        if (dimension != ChunkUtils.getActualDimension()) return Collections.emptyList();
        return pathLines;
    }
}
