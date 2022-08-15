package xaeroplus.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import xaero.map.region.LeveledRegion;
import xaero.map.region.MapTileChunk;
import xaero.map.region.texture.LeafRegionTexture;
import xaero.map.region.texture.RegionTexture;

import static xaeroplus.XaeroPlus.MAX_LEVEL;

@Mixin(value = LeafRegionTexture.class, remap = false)
public abstract class MixinLeafRegionTexture extends RegionTexture<LeafRegionTexture> {

    @Shadow
    private MapTileChunk tileChunk;

    // ignored
    public MixinLeafRegionTexture(LeveledRegion<LeafRegionTexture> region) {
        super(region);
    }

    /**
     * @author rfresh2
     * @reason Remove hardcoded max cache level
     */
    @Overwrite
    public void deleteTexturesAndBuffers() {
        if (!Minecraft.getMinecraft().isCallingFromMinecraftThread()) {
            LeveledRegion leveledRegion = this.region.getLevel() == MAX_LEVEL ? this.region : this.region.getParent();
            synchronized (leveledRegion) {
                LeveledRegion leveledRegion2 = this.region;
                synchronized (leveledRegion2) {
                    this.tileChunk.setLoadState((byte)0);
                }
            }
        }
        super.deleteTexturesAndBuffers();
    }
}
