package xaeroplus.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.MapProcessor;
import xaeroplus.XaeroPlus;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.util.DataFolderResolveUtil;
import xaeroplus.util.Globals;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Mixin(value = MapProcessor.class, remap = false)
public abstract class MixinMapProcessor {
    @Shadow
    private String currentWorldId;
    @Shadow
    private String currentDimId;
    @Shadow
    private String currentMWId;

    @Inject(method = "getMainId(I)Ljava/lang/String;", at = @At("HEAD"), cancellable = true)
    private void getMainId(final int version, final CallbackInfoReturnable<String> cir) {
        DataFolderResolveUtil.resolveDataFolder(cir);
    }

    @Inject(method = "getDimensionName", at = @At(value = "HEAD"), cancellable = true)
    public void getDimensionName(final int id, final CallbackInfoReturnable<String> cir) {
        if (!Globals.nullOverworldDimensionFolder) {
            cir.setReturnValue("DIM" + id);
        }
    }

    @Unique
    private final static String LOCK_ID = UUID.randomUUID().toString();

    @Redirect(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"
    ))
    public Path replaceLockPath(final Path path, final String other) {
        return Paths.get(System.getProperty("java.io.tmpdir")).resolve(LOCK_ID + ".lock");
    }

    @Inject(method = "updateWorldSynced", at = @At(
        value = "INVOKE",
        target = "Lxaero/map/MapProcessor;popRenderPause(ZZ)V",
        ordinal = 0
    ))
    public void fireWorldChangedEvent(final CallbackInfo ci) {
        XaeroPlus.EVENT_BUS.post(new XaeroWorldChangeEvent(this.currentWorldId, this.currentDimId, this.currentMWId));
    }
}
