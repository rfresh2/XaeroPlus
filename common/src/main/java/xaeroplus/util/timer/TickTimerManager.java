package xaeroplus.util.timer;

import net.lenni0451.lambdaevents.EventHandler;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;

public final class TickTimerManager {
    // before any modules
    public static final int TICK_PRIORITY = Integer.MAX_VALUE - 1;
    public static final TickTimerManager INSTANCE = new TickTimerManager();

    private volatile long tickTime = 0;

    private TickTimerManager() {
        XaeroPlus.EVENT_BUS.register(this);
    }

    public long getTickTime() {
        return tickTime;
    }

    @EventHandler(priority = TICK_PRIORITY)
    public void onClientTick(ClientTickEvent.Pre event) {
        tickTime++;
    }
}
