package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.mods.SupportXaeroMinimap;
import xaero.map.mods.gui.Waypoint;
import xaeroplus.settings.Settings;

import java.util.ArrayList;

@Mixin(value = SupportXaeroMinimap.class, remap = false)
public class MixinSupportXaeroMinimap {

    @Redirect(method = "waypointExists", at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;contains(Ljava/lang/Object;)Z"))
    public boolean waypointEqualityRedirect(final ArrayList waypoints, final Object w) {
        // only needed because refreshing waypoints while a drop down menu exists means waypoint object references change
        // and xaero didn't implement equals or hashcode for waypoints lol
        try {
            final ArrayList<Waypoint> wList = (ArrayList<Waypoint>) waypoints;
            final Waypoint waypoint = (Waypoint) w;
            for (final Waypoint w2 : wList) {
                if (w2.compareTo(waypoint) == 0) {
                    return true;
                }
            }
            return false;
        } catch (final Exception e) {
            return waypoints.contains(w);
        }
    }

    @Inject(method = "getSubWorldNameToRender", at = @At("HEAD"), cancellable = true)
    public void getSubworldNameToRenderInject(final CallbackInfoReturnable<String> cir) {
        if (Settings.REGISTRY.owAutoWaypointDimension.get()) {
            // remove annoying string rendered in the middle of the worldmap
            cir.setReturnValue(null);
        }
    }
}
