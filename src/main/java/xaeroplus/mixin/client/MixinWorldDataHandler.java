package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapRegion;
import xaeroplus.util.CustomWorldDataHandler;
import xaeroplus.util.Shared;

import java.io.File;

@Mixin(value = WorldDataHandler.class, remap = false)
public class MixinWorldDataHandler implements CustomWorldDataHandler {

    @Shadow
    private WorldServer worldServer;

    /**
     * @author rfresh2
     * @reason dimension switching singleplayer support
     */
    @Overwrite
    public WorldServer getWorldServer() {
        return getWorldServer(Shared.customDimensionId);
    }

    public WorldServer getWorldServer(int dimId) {
        if (Minecraft.getMinecraft().isSingleplayer()) {
            return Minecraft.getMinecraft().getIntegratedServer().getWorld(dimId);
        } else {
            return null;
        }
    }

    /**
     * @author rfresh2
     * @reason dimension switching singleplayer support
     */
    @Overwrite
    public File getWorldDir() {
        WorldServer server = getWorldServer();
        if (server != null) {
            return server.getChunkSaveLocation();
        } else {
            return null;
        }
    }

    @Override
    public File getWorldDir(int dimId) {
        WorldServer server = getWorldServer(dimId);
        if (server != null) {
            return server.getChunkSaveLocation();
        } else {
            return null;
        }
    }

    @Inject(method = "buildRegion", at = @At("HEAD"))
    public void buildRegionInject(final MapRegion region, final World world, final boolean loading, final int[] chunkCountDest, final CallbackInfoReturnable<WorldDataHandler.Result> cir) {
        // this var is used after in an if-else condition, so we need to set it here
        this.worldServer = getWorldServer(region.getDim().getDimId());
    }

    @Redirect(method = "buildRegion", at = @At(value = "INVOKE", target = "Lxaero/map/file/worldsave/WorldDataReader;buildRegion(Lxaero/map/region/MapRegion;Ljava/io/File;Lnet/minecraft/world/World;Z[ILxaero/map/executor/Executor;Lnet/minecraft/world/WorldServer;)Z"))
    public boolean buildRegionRedirect(final WorldDataReader worldDataReader, final MapRegion region, final File file, final World world, final boolean loading, final int[] chunkCountDest, final xaero.map.executor.Executor executor, final WorldServer worldServer) {
        final WorldServer server = getWorldServer(region.getDim().getDimId());
        return worldDataReader.buildRegion(region, server.getChunkSaveLocation(), server, loading, chunkCountDest, executor, server);
    }
}
