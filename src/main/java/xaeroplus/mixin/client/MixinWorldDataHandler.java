package xaeroplus.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.map.executor.Executor;
import xaero.map.file.worldsave.WorldDataHandler;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapRegion;
import xaeroplus.util.CustomWorldDataHandler;
import xaeroplus.util.Shared;

import java.nio.file.Path;

@Mixin(value = WorldDataHandler.class, remap = false)
public class MixinWorldDataHandler implements CustomWorldDataHandler {

    @Shadow
    private ServerWorld worldServer;

    /**
     * @author rfresh2
     * @reason dimension switching singleplayer support
     */
    @Overwrite
    public ServerWorld getWorldServer() {
        return getWorldServer(Shared.customDimensionId);
    }

    public ServerWorld getWorldServer(RegistryKey<World> dimId) {
        if (MinecraftClient.getInstance().isInSingleplayer()) {
            return MinecraftClient.getInstance().getServer().getWorld(dimId);
        } else {
            return null;
        }
    }

    /**
     * @author rfresh2
     * @reason dimension switching singleplayer support
     */
    @Overwrite
    public Path getWorldDir() {
        ServerWorld server = getWorldServer();
        if (server != null) {
            return DimensionType.getSaveDirectory(Shared.customDimensionId, server.getServer().getSavePath(WorldSavePath.ROOT));
        } else {
            return null;
        }
    }

    @Override
    public Path getWorldDir(RegistryKey<World> dimId) {
        ServerWorld server = getWorldServer(dimId);
        if (server != null) {
            return DimensionType.getSaveDirectory(dimId, server.getServer().getSavePath(WorldSavePath.ROOT));
        } else {
            return null;
        }
    }

    @Inject(method = "buildRegion", at = @At("HEAD"))
    public void buildRegionInject(final MapRegion region,
                                  final World world,
                                  final RegistryWrapper<Block> blockLookup,
                                  final Registry<Block> blockRegistry,
                                  final Registry<Fluid> fluidRegistry,
                                  final boolean loading,
                                  final int[] chunkCountDest,
                                  final CallbackInfoReturnable<WorldDataHandler.Result> cir) {
        // this var is used after in an if-else condition, so we need to set it here
        this.worldServer = getWorldServer(region.getDim().getDimId());
    }

    @Redirect(method = "buildRegion", at = @At(value = "INVOKE", target = "Lxaero/map/file/worldsave/WorldDataReader;buildRegion(Lxaero/map/region/MapRegion;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/World;Lnet/minecraft/registry/RegistryWrapper;Lnet/minecraft/registry/Registry;Lnet/minecraft/registry/Registry;Z[ILxaero/map/executor/Executor;)Z"))
    public boolean buildRegionRedirect(final WorldDataReader worldDataReader,
                                       final MapRegion region,
                                       final ServerWorld serverWorld,
                                       final World world,
                                       final RegistryWrapper<Block> worldBottomY,
                                       final Registry<Block> worldTopY,
                                       final Registry<Fluid> chunkManager,
                                       final boolean biomeRegistry,
                                       final int[] theVoid,
                                       final Executor lastFuture) {
        final ServerWorld customServer = getWorldServer(region.getDim().getDimId());
        ServerWorld customWorld = customServer.getServer().getWorld(region.getDim().getDimId());
        return worldDataReader.buildRegion(region,
                                           customServer,
                                           customWorld,
                                           worldBottomY,
                                           worldTopY,
                                           chunkManager,
                                           biomeRegistry,
                                           theVoid,
                                           lastFuture);
    }
}
