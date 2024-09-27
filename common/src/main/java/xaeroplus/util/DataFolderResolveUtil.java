package xaeroplus.util;

import com.google.common.net.InternetDomainName;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.core.XaeroWorldMapCore;
import xaero.map.file.MapSaveLoad;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.Settings;

import java.nio.file.Path;

import static java.util.Objects.nonNull;

public class DataFolderResolveUtil {

    public static void resolveDataFolder(final ClientPacketListener connection, final CallbackInfoReturnable<String> cir) {
        final Settings.DataFolderResolutionMode dataFolderResolutionMode = Globals.dataFolderResolutionMode;
        if (dataFolderResolutionMode == Settings.DataFolderResolutionMode.SERVER_NAME) {
            if (nonNull(connection.getServerData())) {
                String serverName = connection.getServerData().name;
                if (!serverName.isEmpty()) {
                    // use common directories based on server list name instead of IP
                    // good for proxies
                    cir.setReturnValue(sanitizeDataFolderName("Multiplayer_" + serverName));
                    cir.cancel();
                    return;
                }
            }
            // expected case if we're in singleplayer
            if (!Minecraft.getInstance().hasSingleplayerServer()) {
                XaeroPlus.LOGGER.error("Unable to resolve valid MC Server Name. Falling back to default Xaero data folder resolution");
            }
        } else if (dataFolderResolutionMode == Settings.DataFolderResolutionMode.BASE_DOMAIN) {
            if (nonNull(connection.getServerData())) {
                // use the base domain name, e.g connect.2b2t.org -> 2b2t.org
                String id;
                try {
                    id = InternetDomainName.from(connection.getServerData().ip).topPrivateDomain().toString();
                } catch (IllegalArgumentException ex) { // not a domain
                    // fallback to default resolution behavior.
                    // can occur when IP has a port in it. e.g. "localhost:25565"
                    XaeroPlus.LOGGER.error("Error resolving BASE_DOMAIN data folder. Falling back to default Xaero resolution.", ex);
                    return;
                }
                while(id.endsWith(".")) {
                    id = id.substring(0, id.length() - 1);
                }
                if (!id.isEmpty()) {
                    id = "Multiplayer_" + id;
                    cir.setReturnValue(sanitizeDataFolderName(id));
                    cir.cancel();
                    return;
                }
                // expected case if we're in singleplayer
                if (!Minecraft.getInstance().hasSingleplayerServer()) {
                    XaeroPlus.LOGGER.error("Unable to resolve valid Base domain. Falling back to default Xaero data folder resolution");
                }
            }
        }
    }

    public static Component getCurrentDataDirPath() {
        try {
            WorldMapSession currentSession = XaeroWorldMapCore.currentSession;
            MapProcessor mapProcessor = currentSession.getMapProcessor();
            String mainId = mapProcessor.getMapWorld().getMainId();
            Path rootFolder = MapSaveLoad.getRootFolder(mainId);
            return Component.literal(rootFolder.toString())
                .append(Component.literal(" (")
                            .append(Component.literal("Click To Open").withStyle(style -> style
                                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, rootFolder.toString()))
                                .withColor(ChatFormatting.GOLD)))
                            .append(Component.literal(")")));
        } catch (final Throwable e) {
            XaeroPlus.LOGGER.error("Failed to get data directory", e);
            return Component.literal("Failed to get data directory");
        }
    }

    public static String sanitizeDataFolderName(final String in) {
        final String invalidChars = "[<>:\"/\\|?*]";
        // Replace invalid characters with underscores
        return in.replaceAll(invalidChars, "_");
    }
}
