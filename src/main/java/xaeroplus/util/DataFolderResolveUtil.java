package xaeroplus.util;

import com.google.common.net.InternetDomainName;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaeroplus.XaeroPlus;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import static java.util.Objects.nonNull;

public class DataFolderResolveUtil {

    public static void resolveDataFolder(final CallbackInfoReturnable<String> cir) {
        final XaeroPlusSettingRegistry.DataFolderResolutionMode dataFolderResolutionMode = XaeroPlus.dataFolderResolutionMode;
        if (dataFolderResolutionMode == XaeroPlusSettingRegistry.DataFolderResolutionMode.SERVER_NAME) {
            Minecraft mc = Minecraft.getMinecraft();
            if (nonNull(mc.getCurrentServerData())) {
                String serverName = mc.getCurrentServerData().serverName.replace(":", "_");
                while(serverName.endsWith(".")) {
                    serverName = serverName.substring(0, serverName.length() - 1);
                }
                if (serverName.length() > 0) {
                    // use common directories based on server list name instead of IP
                    // good for proxies
                    cir.setReturnValue("Multiplayer_" + serverName);
                    cir.cancel();
                    return;
                }
            }
            XaeroPlus.LOGGER.error("Unable to resolve valid MC Server Name. Falling back to default Xaero data folder resolution");
        } else if (dataFolderResolutionMode == XaeroPlusSettingRegistry.DataFolderResolutionMode.BASE_DOMAIN) {
            Minecraft mc = Minecraft.getMinecraft();
            if (nonNull(mc.getCurrentServerData())) {
                // use the base domain name, e.g connect.2b2t.org -> 2b2t.org
                String id;
                try {
                    id = InternetDomainName.from(mc.getCurrentServerData().serverIP).topPrivateDomain().toString();
                } catch (IllegalArgumentException ex) { // not a domain
                    // fallback to default resolution behavior.
                    // can occur when IP has a port in it. e.g. "localhost:25565"
                    XaeroPlus.LOGGER.error("Error resolving BASE_DOMAIN data folder. Falling back to default Xaero resolution.", ex);
                    return;
                }
                id = id.replace(":", "_");
                while(id.endsWith(".")) {
                    id = id.substring(0, id.length() - 1);
                }
                if (id.length() > 0) {
                    id = "Multiplayer_" + id;
                    cir.setReturnValue(id);
                    cir.cancel();
                    return;
                }
                XaeroPlus.LOGGER.error("Unable to resolve valid Base domain. Falling back to default Xaero data folder resolution");
            }
        }
    }
}
