package xaeroplus.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.DataFolderResolveUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

import static xaeroplus.Globals.LOCK_ID;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor {
    @Shadow private String currentWorldId;
    @Shadow private String currentDimId;
    @Shadow private String currentMWId;

    @Inject(method = "getMainId", at = @At("HEAD"), cancellable = true, remap = false)
    private void getMainId(final boolean rootFolderFormat, boolean preIP6Fix, final ClientPacketListener connection, final CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(connection, cir);
    }

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true, remap = false)
    public void getDimensionName(final ResourceKey<Level> id, final CallbackInfoReturnable<String> cir) {
        if (!Globals.nullOverworldDimensionFolder) {
            if (id == Level.OVERWORLD) {
                cir.setReturnValue("DIM0");
            }
        }
    }

    @Redirect(method = "run", at = @At(
        value = "INVOKE",
        target = "Ljava/lang/Thread;sleep(J)V"
    ))
    public void decreaseThreadSleepTime(final long millis) throws InterruptedException {
        Thread.sleep(5L);
    }

    @Redirect(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"
    ))
    public Path replaceLockPath(final Path instance, final String other) {
        return Paths.get(System.getProperty("java.io.tmpdir")).resolve(LOCK_ID + ".lock");
    }

    @Inject(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/MapProcessor;popRenderPause(ZZ)V",
        ordinal = 0
    ))
    public void fireWorldChangedEvent(final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.call(new XaeroWorldChangeEvent(this.currentWorldId, this.currentDimId, this.currentMWId));
    }
}
