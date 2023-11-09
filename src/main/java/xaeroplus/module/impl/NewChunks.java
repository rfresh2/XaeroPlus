package xaeroplus.module.impl;

import com.collarmc.pounce.Subscribe;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.mixin.client.mc.AccessorWorldRenderer;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.DrawManager;
import xaeroplus.util.Shared;
import xaeroplus.util.highlights.ChunkHighlightCache;
import xaeroplus.util.highlights.ChunkHighlightLocalCache;
import xaeroplus.util.highlights.ChunkHighlightSavingCache;

import java.util.concurrent.TimeUnit;

import static net.minecraft.world.World.*;
import static xaeroplus.util.ChunkUtils.getActualDimension;
import static xaeroplus.util.ColorHelper.getColor;

@Module.ModuleInfo()
public class NewChunks extends Module {
    // todo: add newchunk detection mode setting: fluid flows (current), timing, etc.

    private ChunkHighlightCache newChunksCache = new ChunkHighlightLocalCache();
    private final Cache<Long, Byte> oldChunksCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();
    private int newChunksColor = getColor(255, 0, 0, 100);
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Direction[] searchDirs = new Direction[] { Direction.EAST, Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.UP };
    private static final String DATABASE_NAME = "XaeroPlusNewChunks";

    public void setNewChunksCache(boolean disk) {
        try {
            final Long2LongMap map = newChunksCache.getHighlightsState();
            newChunksCache.onDisable();
            if (disk) {
                newChunksCache = new ChunkHighlightSavingCache(DATABASE_NAME);
            } else {
                newChunksCache = new ChunkHighlightLocalCache();
            }
            if (this.isEnabled()) {
                newChunksCache.onEnable();
                if (map != null) newChunksCache.loadPreviousState(map);
            }
        } catch (final Exception e) {
            XaeroPlus.LOGGER.error("Error closing new chunks cache", e);
        }
    }

    @Subscribe
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        if (mc.world == null || ((AccessorWorldRenderer) mc.worldRenderer).getChunks() == null) return;
        // credits to BleachHack for this fluid flow based detection method
        if (event.packet() instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkDeltaUpdateS2CPacket packet = (ChunkDeltaUpdateS2CPacket) event.packet();

            packet.visitUpdates((pos, state) -> {
                if (!state.getFluidState().isEmpty() && !state.getFluidState().isStill()) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    for (Direction dir: searchDirs) {
                        if (mc.world.getBlockState(pos.offset(dir)).getFluidState().isStill()
                                && oldChunksCache.getIfPresent(ChunkUtils.chunkPosToLong(chunkPos)) == null) {
                            newChunksCache.addHighlight(chunkPos.x, chunkPos.z);
                            return;
                        }
                    }
                }
            });
        } else if (event.packet() instanceof BlockUpdateS2CPacket) {
            BlockUpdateS2CPacket packet = (BlockUpdateS2CPacket) event.packet();

            if (!packet.getState().getFluidState().isEmpty() && !packet.getState().getFluidState().isStill()) {
                ChunkPos chunkPos = new ChunkPos(packet.getPos());

                for (Direction dir: searchDirs) {
                    if (mc.world.getBlockState(packet.getPos().offset(dir)).getFluidState().isStill()
                            && oldChunksCache.getIfPresent(ChunkUtils.chunkPosToLong(chunkPos)) == null) {
                        newChunksCache.addHighlight(chunkPos.x, chunkPos.z);
                        return;
                    }
                }
            }
        } else if (event.packet() instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacket packet = (ChunkDataS2CPacket) event.packet();

            ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());

            if (!newChunksCache.isHighlighted(pos.x, pos.z, getActualDimension()) && mc.world.getChunkManager().getChunk(packet.getX(), packet.getZ()) == null) {
                WorldChunk chunk = new WorldChunk(mc.world, pos);
                try {
                    chunk.loadFromPacket(packet.getChunkData().getSectionsDataBuf(), new NbtCompound(), packet.getChunkData().getBlockEntities(packet.getX(), packet.getZ()));
                } catch (Throwable e) {
                    return;
                }

                for (int x = 0; x < 16; x++) {
                    for (int y = mc.world.getBottomY(); y < mc.world.getTopY(); y++) {
                        for (int z = 0; z < 16; z++) {
                            FluidState fluid = chunk.getFluidState(x, y, z);

                            if (!fluid.isEmpty() && !fluid.isStill()) {
                                oldChunksCache.put(ChunkUtils.chunkPosToLong(pos), (byte) 0);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onXaeroWorldChangeEvent(final XaeroWorldChangeEvent event) {
        if (XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.getValue()) {
            if (inUnknownDimension() && newChunksCache instanceof ChunkHighlightSavingCache) {
                XaeroPlusSettingRegistry.newChunksSaveLoadToDisk.setValue(false);
                XaeroPlus.LOGGER.warn("Entered unknown dimension with saving cache on, disabling disk saving");
            }
        }
        newChunksCache.handleWorldChange();
    }

    public boolean inUnknownDimension() {
        final RegistryKey<World> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @Subscribe
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        newChunksCache.handleTick();
    }

    @Override
    public void onEnable() {
        Shared.drawManager.registerChunkHighlightDrawFeature(
            this.getClass(),
            new DrawManager.ChunkHighlightDrawFeature(
                this::isEnabled,
                this::isNewChunk,
                this::getNewChunksColor
            ));
        newChunksCache.onEnable();
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
    }

    public int getNewChunksColor() {
        return newChunksColor;
    }

    public void setRgbColor(final int color) {
        newChunksColor = ColorHelper.getColorWithAlpha(color, (int) XaeroPlusSettingRegistry.newChunksAlphaSetting.getValue());
    }

    public void setAlpha(final float a) {
        newChunksColor = ColorHelper.getColorWithAlpha(newChunksColor, (int) (a));
    }

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final RegistryKey<World> dimensionId) {
        return newChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
