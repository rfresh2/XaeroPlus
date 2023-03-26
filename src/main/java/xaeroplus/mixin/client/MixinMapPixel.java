package xaeroplus.mixin.client;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.MapProcessor;
import xaero.map.MapWriter;
import xaero.map.WorldMap;
import xaero.map.biome.BiomeColorCalculator;
import xaero.map.cache.BlockStateShortShapeCache;
import xaero.map.misc.Misc;
import xaero.map.region.*;

import java.util.ArrayList;
@Mixin(value = MapPixel.class, remap = false)
public abstract class MixinMapPixel {

    @Shadow
    @Final
    private static int VOID_COLOR;
    @Final
    @Shadow
    private static float DEFAULT_AMBIENT_LIGHT;
    @Final
    @Shadow
    private static float DEFAULT_AMBIENT_LIGHT_COLORED;
    @Final
    @Shadow
    private static float DEFAULT_AMBIENT_LIGHT_WHITE;
    @Final
    @Shadow
    private static float DEFAULT_MAX_DIRECT_LIGHT;
    @Final
    @Shadow
    private static float GLOWING_MAX_DIRECT_LIGHT;
    @Shadow
    protected int state = 0;
    @Shadow
    protected byte colourType;
    @Shadow
    protected int customColour;
    @Shadow
    protected byte light;
    @Shadow
    protected boolean glowing;
    @Shadow
    public abstract float getBlockBrightness(float min, int l, int sun);
    @Shadow
    public abstract float getPixelLight(float min, int topLightValue);
    @Shadow
    public abstract int getState();
    @Shadow
    public abstract void setState(int state);
    @Shadow
    public abstract void setLight(byte light);
    @Shadow
    public abstract void setGlowing(boolean glowing);
    @Shadow
    public abstract byte getColourType();
    @Shadow
    public abstract void setColourType(byte colourType);
    @Shadow
    public abstract int getCustomColour();
    @Shadow
    public abstract void setCustomColour(int customColour);

    @Inject(method = "getPixelColours", at = @At("HEAD"), cancellable = true)
    public void getPixelColours(
            int[] result_dest,
            MapWriter mapWriter,
            World world,
            MapTileChunk tileChunk,
            MapTileChunk prevChunk,
            MapTileChunk prevChunkDiagonal,
            MapTileChunk prevChunkHorisontal,
            MapTile mapTile,
            int x,
            int z,
            MapBlock block,
            int height,
            ArrayList<Overlay> overlays,
            BlockPos.MutableBlockPos mutableGlobalPos,
            float shadowR,
            float shadowG,
            float shadowB,
            BiomeColorCalculator biomeColorCalculator,
            MapProcessor mapProcessor,
            OverlayManager overlayManager,
            BlockStateShortShapeCache blockStateShortShapeCache,
            CallbackInfo ci
    ) {
        int colour = block != null && block.isCaveBlock() ? 0 : -16121833;
        int topLightValue = this.light;
        int lightMin = 9;
        float brightnessR = 1.0F;
        float brightnessG = 1.0F;
        float brightnessB = 1.0F;
        mutableGlobalPos.setPos(mapTile.getChunkX() * 16 + x, height, mapTile.getChunkZ() * 16 + z);
        int state = this.state;
        IBlockState blockState = Misc.getStateById(state);
        boolean isAir = blockState.getBlock() instanceof BlockAir;
        boolean isFinalBlock = (MapPixel) (Object) this instanceof MapBlock;
        if (!isAir) {
            if (WorldMap.settings.colours == 0) {
                colour = mapWriter.loadBlockColourFromTexture(state, true, world, mutableGlobalPos);
            } else {
                try {
                    Block b = blockState.getBlock();
                    int a = b instanceof BlockLiquid ? 191 : (b instanceof BlockIce ? 216 : (b instanceof BlockObsidian ? 216 : 127));
                    colour = blockState.getMapColor(world, mutableGlobalPos).colorValue;
                    if (!isFinalBlock && colour == 0) {
                        result_dest[0] = -1;
                        return;
                    }

                    colour = a << 24 | colour & 16777215;
                } catch (Exception var51) {
                }
            }

            if (!isFinalBlock
                    && !WorldMap.settings.displayStainedGlass
                    && (blockState.getBlock() instanceof BlockStainedGlass || blockState.getBlock() instanceof BlockStainedGlassPane)) {
                result_dest[0] = -1;
                return;
            }
        }

        int r = colour >> 16 & 0xFF;
        int g = colour >> 8 & 0xFF;
        int b = colour & 0xFF;
        if (this.colourType == -1) {
            if (!isFinalBlock) {
                throw new RuntimeException("Can't modify colour type stuff for overlays!");
            }

            mapWriter.getColorTypeCache().getBlockBiomeColour(world, blockState, mutableGlobalPos, result_dest, block.getBiome());
            this.colourType = (byte)result_dest[0];
            if (result_dest[1] != -1) {
                block.setBiome(result_dest[1]);
            }

            this.customColour = result_dest[2];
        }

        if (this.colourType != 0 && (WorldMap.settings.biomeColorsVanillaMode || WorldMap.settings.colours == 0)) {
            int c = this.customColour;
            if (this.colourType == 1 || this.colourType == 2) {
                c = biomeColorCalculator.getBiomeColor(blockState, !isFinalBlock, mutableGlobalPos, mapTile, world, mapProcessor, mapWriter.getColorTypeCache());
            }

            float rMultiplier = (float)r / 255.0F;
            float gMultiplier = (float)g / 255.0F;
            float bMultiplier = (float)b / 255.0F;
            r = (int)((float)(c >> 16 & 0xFF) * rMultiplier);
            g = (int)((float)(c >> 8 & 0xFF) * gMultiplier);
            b = (int)((float)(c & 0xFF) * bMultiplier);
        }

        if (this.glowing) {
            int total = r + g + b;
            float minBrightness = 407.0F;
            float brightener = Math.max(1.0F, minBrightness / (float)total);
            r = (int)((float)r * brightener);
            g = (int)((float)g * brightener);
            b = (int)((float)b * brightener);
            topLightValue = 15;
        }

        int overlayRed = 0;
        int overlayGreen = 0;
        int overlayBlue = 0;
        float currentTransparencyMultiplier = 1.0F;
        if (overlays != null && !overlays.isEmpty()) {
            int sun = 15;
            boolean hasValidOverlay = false;

            for(int i = 0; i < overlays.size(); ++i) {
                Overlay o = overlays.get(i);
                if (o.getColourType() == -1) {
                    mapWriter.getColorTypeCache().getBlockBiomeColour(world, Misc.getStateById(o.getState()), mutableGlobalPos, result_dest, block.getBiome());
                    int overlayColourType = (byte)result_dest[0];
                    if (overlayColourType == -1) {
                        continue;
                    }

                    int overlayCustomColour = result_dest[2];
                    o = overlayManager.getOriginal(o, overlayColourType, overlayCustomColour);
                    overlays.set(i, o);
                }

                o.getPixelColour(
                        block,
                        result_dest,
                        mapWriter,
                        world,
                        tileChunk,
                        prevChunk,
                        prevChunkDiagonal,
                        prevChunkHorisontal,
                        mapTile,
                        x,
                        z,
                        mutableGlobalPos,
                        shadowR,
                        shadowG,
                        shadowB,
                        biomeColorCalculator,
                        mapProcessor,
                        overlayManager
                );
                if (result_dest[0] != -1) {
                    hasValidOverlay = true;
                    if (i == 0) {
                        topLightValue = ((IMixinMapPixel)o).getLight();
                    }

                    float transparency = (float)result_dest[3] / 255.0F;
                    float overlayIntensity = this.getBlockBrightness((float)lightMin, ((IMixinMapPixel)o).getLight(), sun) * transparency * currentTransparencyMultiplier;
                    overlayRed = (int)((float)overlayRed + (float)result_dest[0] * overlayIntensity);
                    overlayGreen = (int)((float)overlayGreen + (float)result_dest[1] * overlayIntensity);
                    overlayBlue = (int)((float)overlayBlue + (float)result_dest[2] * overlayIntensity);
                    sun -= o.getOpacity();
                    if (sun < 0) {
                        sun = 0;
                    }

                    currentTransparencyMultiplier *= 1.0F - transparency;
                }
            }

            if (hasValidOverlay && !this.glowing && !isAir) {
                brightnessR = brightnessG = brightnessB = this.getBlockBrightness((float)lightMin, this.light, sun);
            }
        }

        if (isFinalBlock) {
            if (((IMixinMapBlock)block).getSlopeUnknown()) {
                if (!isAir) {
                    block.fixHeightType(x, z, mapTile, tileChunk, prevChunk, prevChunkDiagonal, prevChunkHorisontal, height, false, blockStateShortShapeCache);
                } else {
                    block.setVerticalSlope((byte)0);
                    block.setDiagonalSlope((byte)0);
                    ((IMixinMapBlock)block).setSlopeUnknown(false);
                }
            }

            float depthBrightness = 1.0F;
            int slopes = WorldMap.settings.terrainSlopes;
            if (!this.glowing && !isAir) {
                boolean caving = block.isCaveBlock() && height != -1 && height < 127;
                float caveBrightness = (float)height / 127.0F;
                if (caving) {
                    brightnessB = caveBrightness;
                    brightnessG = caveBrightness;
                    brightnessR = caveBrightness;
                }

                if (!caving && WorldMap.settings.terrainDepth && height != -1) {
                    depthBrightness = (float)height / 63.0F;
                    float max = slopes >= 2 ? 1.0F : 1.15F;
                    float min = slopes >= 2 ? 0.9F : 0.7F;
                    if (depthBrightness > max) {
                        depthBrightness = max;
                    } else if (depthBrightness < min) {
                        depthBrightness = min;
                    }
                }
            }

            if (!isAir && slopes > 0 && !((IMixinMapBlock)block).getSlopeUnknown()) {
                int verticalSlope = block.getVerticalSlope();
                if (slopes == 1) {
                    if (verticalSlope > 0) {
                        depthBrightness = (float)((double)depthBrightness * 1.15);
                    } else if (verticalSlope < 0) {
                        depthBrightness = (float)((double)depthBrightness * 0.85);
                    }
                } else {
                    int diagonalSlope = block.getDiagonalSlope();
                    float ambientLightColored = 0.2F;
                    float ambientLightWhite = 0.5F;
                    float maxDirectLight = 0.6666667F;
                    if (this.glowing) {
                        ambientLightColored = 0.0F;
                        ambientLightWhite = 1.0F;
                        maxDirectLight = 0.22222224F;
                    }

                    float cos = 0.0F;
                    if (slopes == 2) {
                        float crossZ = (float)(-verticalSlope);
                        if (crossZ < 1.0F) {
                            if (verticalSlope == 1 && diagonalSlope == 1) {
                                cos = 1.0F;
                            } else {
                                float crossX = (float)(verticalSlope - diagonalSlope);
                                float cast = 1.0F - crossZ;
                                float crossMagnitude = (float)Math.sqrt((double)(crossX * crossX + 1.0F + crossZ * crossZ));
                                cos = (float)((double)(cast / crossMagnitude) / Math.sqrt(2.0));
                            }
                        }
                    } else if (verticalSlope >= 0) {
                        if (verticalSlope == 1) {
                            cos = 1.0F;
                        } else {
                            float surfaceDirectionMagnitude = (float)Math.sqrt((double)(verticalSlope * verticalSlope + 1));
                            float castToMostLit = (float)(verticalSlope + 1);
                            cos = (float)((double)(castToMostLit / surfaceDirectionMagnitude) / Math.sqrt(2.0));
                        }
                    }

                    float directLightClamped = 0.0F;
                    if (cos == 1.0F) {
                        directLightClamped = maxDirectLight;
                    } else if (cos > 0.0F) {
                        directLightClamped = (float)Math.ceil((double)(cos * 10.0F)) / 10.0F * maxDirectLight * 0.88388F;
                    }

                    float whiteLight = ambientLightWhite + directLightClamped;
                    brightnessR *= shadowR * ambientLightColored + whiteLight;
                    brightnessG *= shadowG * ambientLightColored + whiteLight;
                    brightnessB *= shadowB * ambientLightColored + whiteLight;
                }
            }

            brightnessR *= depthBrightness;
            brightnessG *= depthBrightness;
            brightnessB *= depthBrightness;
            result_dest[3] = (int)(this.getPixelLight((float)lightMin, topLightValue) * 255.0F);
        } else {
            result_dest[3] = colour >> 24 & 0xFF;
        }

        result_dest[0] = (int)((float)r * brightnessR * currentTransparencyMultiplier + (float)overlayRed);
        if (result_dest[0] > 255) {
            result_dest[0] = 255;
        }

        result_dest[1] = (int)((float)g * brightnessG * currentTransparencyMultiplier + (float)overlayGreen);
        if (result_dest[1] > 255) {
            result_dest[1] = 255;
        }

        result_dest[2] = (int)((float)b * brightnessB * currentTransparencyMultiplier + (float)overlayBlue);
        if (result_dest[2] > 255) {
            result_dest[2] = 255;
        }
    }




}
