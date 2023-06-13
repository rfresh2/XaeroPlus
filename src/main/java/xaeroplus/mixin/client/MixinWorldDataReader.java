package xaeroplus.mixin.client;

import net.minecraft.block.Block;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xaero.map.file.worldsave.WorldDataReader;
import xaero.map.region.MapTile;
import xaero.map.region.MapTileChunk;
import xaeroplus.settings.XaeroPlusSettingRegistry;

import static net.minecraft.world.World.NETHER;

@Mixin(value = WorldDataReader.class, remap = false)
public abstract class MixinWorldDataReader {

    @Shadow
    protected abstract boolean buildTile(
            NbtCompound nbttagcompound,
            MapTile tile,
            MapTileChunk tileChunk,
            int chunkX,
            int chunkZ,
            int insideRegionX,
            int insideRegionZ,
            int caveStart,
            int caveDepth,
            boolean worldHasSkylight,
            boolean ignoreHeightmaps,
            World world,
            RegistryWrapper<Block> blockLookup,
            Registry<Block> blockRegistry,
            Registry<Fluid> fluidRegistry,
            Registry<Biome> biomeRegistry,
            boolean flowers,
            int worldBottomY,
            int worldTopY
    );

    @Redirect(method = "buildTileChunk", at = @At(value = "INVOKE", target = "Lxaero/map/file/worldsave/WorldDataReader;buildTile(Lnet/minecraft/nbt/NbtCompound;Lxaero/map/region/MapTile;Lxaero/map/region/MapTileChunk;IIIIIIZZLnet/minecraft/world/World;Lnet/minecraft/registry/RegistryWrapper;Lnet/minecraft/registry/Registry;Lnet/minecraft/registry/Registry;Lnet/minecraft/registry/Registry;ZII)Z"))
    public boolean redirectBuildTile(final WorldDataReader instance,
                                     NbtCompound nbttagcompound,
                                     MapTile tile,
                                     MapTileChunk tileChunk,
                                     int chunkX,
                                     int chunkZ,
                                     int insideRegionX,
                                     int insideRegionZ,
                                     int caveStart,
                                     int caveDepth,
                                     boolean worldHasSkylight,
                                     boolean ignoreHeightmaps,
                                     World world,
                                     RegistryWrapper<Block> blockLookup,
                                     Registry<Block> blockRegistry,
                                     Registry<Fluid> fluidRegistry,
                                     Registry<Biome> biomeRegistry,
                                     boolean flowers,
                                     int worldBottomY,
                                     int worldTopY) {
        if (XaeroPlusSettingRegistry.netherCaveFix.getValue()) {
            boolean cave = caveStart != Integer.MAX_VALUE;
            boolean nether = tileChunk.getInRegion().getDim().getDimId() == NETHER;
            int customCaveStart = caveStart;
            if (!cave && nether) {
                customCaveStart = Integer.MIN_VALUE;
            }
            return buildTile(
                    nbttagcompound,
                    tile,
                    tileChunk,
                    chunkX,
                    chunkZ,
                    insideRegionX,
                    insideRegionZ,
                    customCaveStart,
                    caveDepth,
                    worldHasSkylight,
                    ignoreHeightmaps,
                    world,
                    blockLookup,
                    blockRegistry,
                    fluidRegistry,
                    biomeRegistry,
                    flowers,
                    worldBottomY,
                    worldTopY
            );
        }
        return buildTile(
                nbttagcompound,
                tile,
                tileChunk,
                chunkX,
                chunkZ,
                insideRegionX,
                insideRegionZ,
                caveStart,
                caveDepth,
                worldHasSkylight,
                ignoreHeightmaps,
                world,
                blockLookup,
                blockRegistry,
                fluidRegistry,
                biomeRegistry,
                flowers,
                worldBottomY,
                worldTopY
        );
    }
}
