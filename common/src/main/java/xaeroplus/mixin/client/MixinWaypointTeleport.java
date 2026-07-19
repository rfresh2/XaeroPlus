package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.waypoint.WaypointTeleport;
import xaero.map.WorldMapSession;
import xaero.map.teleport.MapTeleporter;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroTeleportAttemptEvent;
import xaeroplus.settings.Settings;

@Mixin(value = WaypointTeleport.class, remap = false)
public class MixinWaypointTeleport {

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"), cancellable = true)
    public void routeTeleport(
        final Waypoint waypoint,
        final MinimapWorld world,
        final Screen screen,
        final boolean checkCoordinates,
        final CallbackInfo ci
    ) {
        switch (Settings.REGISTRY.minimapWaypointTeleportMode.get()) {
            case UNCHANGED -> {
            }
            case WORLD_MAP -> {
                if (teleportWithWorldMap(waypoint, world, screen)) {
                    ci.cancel();
                }
            }
            case CUSTOM -> {
                teleportWithCustomCommand(waypoint, world);
                ci.cancel();
            }
        }
    }

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendUnsignedCommand(Ljava/lang/String;)Z"
    ), remap = true)
    public void onTeleportAttemptA(CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
    }

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"
    ), remap = true)
    public void onTeleportAttemptB(CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
    }

    @Unique
    private static boolean teleportWithWorldMap(final Waypoint waypoint, final MinimapWorld world, final Screen screen) {
        final WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null || session.getMapProcessor() == null || !session.getMapProcessor().isMapWorldUsable()) {
            return false;
        }
        final var mapWorld = session.getMapProcessor().getMapWorld();
        final Minecraft minecraft = Minecraft.getInstance();
        if (mapWorld == null || minecraft.level == null) {
            return false;
        }
        final ResourceKey<Level> waypointDimension = world.getDimId();
        final ResourceKey<Level> teleportDimension = waypointDimension != null && waypointDimension != minecraft.level.dimension()
            ? waypointDimension
            : null;
        final int y = waypoint.isYIncluded() ? waypoint.getY() : 32767;
        new MapTeleporter().teleport(screen, mapWorld, waypoint.getX(), y, waypoint.getZ(), teleportDimension);
        return true;
    }

    @Unique
    private static void teleportWithCustomCommand(final Waypoint waypoint, final MinimapWorld world) {
        final String template = Settings.REGISTRY.minimapWaypointCustomTeleportCommand.get();
        if (template.isBlank()) {
            return;
        }
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        final ResourceKey<Level> dimension = world.getDimId() != null ? world.getDimId() : minecraft.level.dimension();
        final String command = template
            .replace("{x}", Integer.toString(waypoint.getX()))
            .replace("{y}", waypoint.isYIncluded() ? Integer.toString(waypoint.getY()) : "~")
            .replace("{z}", Integer.toString(waypoint.getZ()))
            .replace("{d}", dimension.location().toString())
            .replace("{name}", waypoint.getLocalizedName())
            .replace("{yaw}", Integer.toString(waypoint.getYaw()));
        minecraft.setScreen(null);
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
        if (command.startsWith("/")) {
            final String unsignedCommand = command.substring(1);
            if (!minecraft.player.connection.sendUnsignedCommand(unsignedCommand)) {
                minecraft.player.connection.sendChat(unsignedCommand);
            }
        } else {
            minecraft.player.connection.sendChat(command);
        }
    }
}
