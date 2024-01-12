package xaeroplus.module.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import net.lenni0451.lambdaevents.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import xaeroplus.Globals;
import xaeroplus.XaeroPlus;
import xaeroplus.event.ClientTickEvent;
import xaeroplus.event.PacketReceivedEvent;
import xaeroplus.event.XaeroWorldChangeEvent;
import xaeroplus.feature.render.ChunkHighlightProvider;
import xaeroplus.feature.render.ColorHelper;
import xaeroplus.feature.render.highlights.ChunkHighlightCache;
import xaeroplus.feature.render.highlights.ChunkHighlightLocalCache;
import xaeroplus.feature.render.highlights.ChunkHighlightSavingCache;
import xaeroplus.mixin.client.mc.AccessorWorldRenderer;
import xaeroplus.module.Module;
import xaeroplus.settings.XaeroPlusSettingRegistry;
import xaeroplus.util.ChunkUtils;

import java.util.concurrent.TimeUnit;

import static net.minecraft.world.level.Level.*;
import static xaeroplus.feature.render.ColorHelper.getColor;
import static xaeroplus.util.ChunkUtils.getActualDimension;

@Module.ModuleInfo()
public class NewChunks extends Module {
    // todo: add newchunk detection mode setting: fluid flows (current), timing, etc.

    private ChunkHighlightCache newChunksCache = new ChunkHighlightLocalCache();
    private final Cache<Long, Byte> oldChunksCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(10L, TimeUnit.SECONDS)
            .build();
    private int newChunksColor = getColor(255, 0, 0, 100);
    private final Minecraft mc = Minecraft.getInstance();
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

    @EventHandler
    public void onPacketReceivedEvent(final PacketReceivedEvent event) {
        if (mc.level == null || ((AccessorWorldRenderer) mc.levelRenderer).getChunks() == null) return;
        // credits to BleachHack for this fluid flow based detection method
        if (event.packet() instanceof ClientboundSectionBlocksUpdatePacket) {
            ClientboundSectionBlocksUpdatePacket packet = (ClientboundSectionBlocksUpdatePacket) event.packet();

            packet.runUpdates((pos, state) -> {
                if (!state.getFluidState().isEmpty() && !state.getFluidState().isSource()) {
                    ChunkPos chunkPos = new ChunkPos(pos);
                    for (Direction dir: searchDirs) {
                        if (mc.level.getBlockState(pos.relative(dir)).getFluidState().isSource()
                                && oldChunksCache.getIfPresent(ChunkUtils.chunkPosToLong(chunkPos)) == null) {
                            newChunksCache.addHighlight(chunkPos.x, chunkPos.z);
                            return;
                        }
                    }
                }
            });
        } else if (event.packet() instanceof ClientboundBlockUpdatePacket) {
            ClientboundBlockUpdatePacket packet = (ClientboundBlockUpdatePacket) event.packet();

            if (!packet.getBlockState().getFluidState().isEmpty() && !packet.getBlockState().getFluidState().isSource()) {
                ChunkPos chunkPos = new ChunkPos(packet.getPos());

                for (Direction dir: searchDirs) {
                    if (mc.level.getBlockState(packet.getPos().relative(dir)).getFluidState().isSource()
                            && oldChunksCache.getIfPresent(ChunkUtils.chunkPosToLong(chunkPos)) == null) {
                        newChunksCache.addHighlight(chunkPos.x, chunkPos.z);
                        return;
                    }
                }
            }
        } else if (event.packet() instanceof ClientboundLevelChunkWithLightPacket) {
            ClientboundLevelChunkWithLightPacket packet = (ClientboundLevelChunkWithLightPacket) event.packet();

            ChunkPos pos = new ChunkPos(packet.getX(), packet.getZ());

            if (!newChunksCache.isHighlighted(pos.x, pos.z, getActualDimension()) && mc.level.getChunkSource().getChunkForLighting(packet.getX(), packet.getZ()) == null) {
                LevelChunk chunk = new LevelChunk(mc.level, pos);
                try {
                    chunk.replaceWithPacketData(packet.getChunkData().getReadBuffer(), new CompoundTag(), packet.getChunkData().getBlockEntitiesTagsConsumer(packet.getX(), packet.getZ()));
                } catch (Throwable e) {
                    return;
                }

                for (int x = 0; x < 16; x++) {
                    for (int y = mc.level.getMinBuildHeight(); y < mc.level.getMaxBuildHeight(); y++) {
                        for (int z = 0; z < 16; z++) {
                            FluidState fluid = chunk.getFluidState(x, y, z);

                            if (!fluid.isEmpty() && !fluid.isSource()) {
                                oldChunksCache.put(ChunkUtils.chunkPosToLong(pos), (byte) 0);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
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
        final ResourceKey<Level> dim = ChunkUtils.getActualDimension();
        return dim != OVERWORLD && dim != NETHER && dim != END;
    }

    @EventHandler
    public void onClientTickEvent(final ClientTickEvent.Post event) {
        newChunksCache.handleTick();
    }

    @Override
    public void onEnable() {
        Globals.drawManager.registerChunkHighlightProvider(
            this.getClass(),
            new ChunkHighlightProvider(
                this::isNewChunk,
                this::getNewChunksColor
            ));
        newChunksCache.onEnable();
    }

    @Override
    public void onDisable() {
        newChunksCache.onDisable();
        Globals.drawManager.unregister(this.getClass());
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

    public boolean isNewChunk(final int chunkPosX, final int chunkPosZ, final ResourceKey<Level> dimensionId) {
        return newChunksCache.isHighlighted(chunkPosX, chunkPosZ, dimensionId);
    }
}
