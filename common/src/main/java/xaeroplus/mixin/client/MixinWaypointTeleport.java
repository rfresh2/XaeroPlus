package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.minimap.waypoint.WaypointTeleport;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroTeleportAttemptEvent;
import xaeroplus.settings.Settings;

@Mixin(value = WaypointTeleport.class, remap = false)
public class MixinWaypointTeleport {
    @Unique
    private static final ThreadLocal<String> xaeroplus$customTeleportCommand = new ThreadLocal<>();

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    public void prepareCustomTeleportCommand(
        final Waypoint waypoint,
        final MinimapWorld world,
        final net.minecraft.client.gui.screens.Screen screen,
        final boolean checkCoordinates,
        final CallbackInfo ci
    ) {
        xaeroplus$customTeleportCommand.remove();
        if (Settings.REGISTRY.useCustomCrossDimensionWaypointTeleportFormat.get()
            && isCrossDimension(world)
            && waypoint != null) {
            final String command = getCustomTeleportCommand(waypoint, world);
            if (command != null) {
                xaeroplus$customTeleportCommand.set(command);
            }
        }
    }

    @Unique
    private static boolean isCrossDimension(final MinimapWorld world) {
        final Minecraft minecraft = Minecraft.getInstance();
        return world != null
            && world.getDimId() != null
            && minecraft.level != null
            && !world.getDimId().equals(minecraft.level.dimension());
    }

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendUnsignedCommand(Ljava/lang/String;)Z"
    ), remap = true)
    private boolean sendTeleportCommand(final ClientPacketListener connection, final String originalCommand) {
        final String customCommand = xaeroplus$customTeleportCommand.get();
        if (customCommand != null && sendCustomCommand(connection, customCommand)) {
            XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
            return true;
        }
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
        return connection.sendUnsignedCommand(originalCommand);
    }

    @Redirect(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"
    ), remap = true)
    private void sendTeleportChat(final ClientPacketListener connection, final String originalCommand) {
        final String customCommand = xaeroplus$customTeleportCommand.get();
        if (customCommand != null && sendCustomCommand(connection, customCommand)) {
            XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
            return;
        }
        XaeroPlus.EVENT_BUS.call(XaeroTeleportAttemptEvent.INSTANCE);
        connection.sendChat(originalCommand);
    }

    @Inject(method = "teleportToWaypoint(Lxaero/common/minimap/waypoints/Waypoint;Lxaero/hud/minimap/world/MinimapWorld;Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("RETURN"))
    private void clearCustomTeleportCommand(
        final Waypoint waypoint,
        final MinimapWorld world,
        final net.minecraft.client.gui.screens.Screen screen,
        final boolean checkCoordinates,
        final CallbackInfo ci
    ) {
        xaeroplus$customTeleportCommand.remove();
    }

    @Unique
    private static String getCustomTeleportCommand(final Waypoint waypoint, final MinimapWorld world) {
        final String template = waypoint.isRotation()
            ? Settings.REGISTRY.crossDimensionWaypointTeleportRotationFormat.get()
            : Settings.REGISTRY.crossDimensionWaypointTeleportFormat.get();
        if (template.isBlank()) {
            return null;
        }
        final ResourceKey<Level> dimension = world.getDimId();
        return template
            .replace("{x}", Integer.toString(waypoint.getX()))
            .replace("{y}", waypoint.isYIncluded() ? Integer.toString(waypoint.getY()) : "~")
            .replace("{z}", Integer.toString(waypoint.getZ()))
            .replace("{d}", dimension.location().toString())
            .replace("{name}", waypoint.getLocalizedName())
            .replace("{yaw}", Integer.toString(waypoint.getYaw()));
    }

    @Unique
    private static boolean sendCustomCommand(final ClientPacketListener connection, final String command) {
        if (command.startsWith("/")) {
            final String unsignedCommand = command.substring(1);
            if (unsignedCommand.isBlank()) {
                return false;
            }
            if (!connection.sendUnsignedCommand(unsignedCommand)) {
                connection.sendCommand(unsignedCommand);
            }
        } else {
            connection.sendChat(command);
        }
        return true;
    }
}
