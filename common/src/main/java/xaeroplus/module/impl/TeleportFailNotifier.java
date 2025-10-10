package xaeroplus.module.impl;

import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.resources.language.I18n;
import xaeroplus.event.ClientTeleportEvent;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.XaeroTeleportAttemptEvent;
import xaeroplus.module.Module;
import xaeroplus.settings.Settings;
import xaeroplus.util.NotificationUtil;
import xaeroplus.util.timer.Timer;
import xaeroplus.util.timer.Timers;

public class TeleportFailNotifier extends Module {
    boolean awaitingTeleport = false;
    Timer timer = Timers.timer();

    @EventHandler
    public void onTeleportAttempt(XaeroTeleportAttemptEvent event) {
        awaitingTeleport = true;
        timer.reset();
    }

    @EventHandler
    public void onClientTeleport(ClientTeleportEvent event) {
        awaitingTeleport = false;
    }

    @EventHandler
    public void onTick(ClientTickEvent.Post event) {
        // players *should* always have perms in singleplayer?
        if (mc.hasSingleplayerServer()) return;
        if (!awaitingTeleport) return;
        if (timer.tick(Settings.REGISTRY.teleportFailNotifierDelay.getAsInt())) {
            awaitingTeleport = false;
            NotificationUtil.errorNotification(I18n.get("xaeroplus.gui.teleport_fail_notifier.message"));
        }
    }
}
