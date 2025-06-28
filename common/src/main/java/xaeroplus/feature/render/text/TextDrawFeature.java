package xaeroplus.feature.render.text;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.util.Mth;
import xaeroplus.Globals;
import xaeroplus.feature.render.DrawContext;
import xaeroplus.feature.render.DrawFeature;
import xaeroplus.util.ChunkUtils;

import static xaeroplus.util.GuiMapHelper.*;

public class TextDrawFeature implements DrawFeature {
    private final TextSupplier textSupplier;
    private final String id;

    public TextDrawFeature(String id, TextSupplier textSupplier) {
        this.id = id;
        this.textSupplier = textSupplier;
    }

    public Long2ObjectMap<Text> getText() {
        final int windowX, windowZ, windowSize;
        var guiMapOptional = getGuiMap();
        if (guiMapOptional.isPresent()) {
            var guiMap = guiMapOptional.get();
            windowX = getGuiMapCenterRegionX(guiMap);
            windowZ = getGuiMapCenterRegionZ(guiMap);
            windowSize = getGuiMapRegionSize(guiMap);
        } else {
            windowX = ChunkUtils.getPlayerRegionX();
            windowZ = ChunkUtils.getPlayerRegionZ();
            windowSize = Math.max(3, Globals.minimapScaleMultiplier);
        }
        return textSupplier.getText(windowX, windowZ, windowSize, Globals.getCurrentDimensionId());
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void render(final DrawContext ctx) {
        var font = Minecraft.getInstance().font;
        var texts = getText();
        var it = Long2ObjectMaps.fastIterator(texts);
        while (it.hasNext()) {
            var text = it.next().getValue();
            ctx.matrixStack().pushPose();
            float textScale = text.scale() * 2.0f * (float) Mth.clamp(
                (ctx.worldmap() ? 1f : Globals.minimapScaleMultiplier) / ctx.fboScale(),
                0.1f * (ctx.worldmap() ? 1.0f : Globals.minimapScaleMultiplier),
                1000f
            );
            float width = font.width(text.value());
            ctx.matrixStack().scale(textScale, textScale, 1);
            ctx.matrixStack().translate(
                text.x() / textScale,
                text.z() / textScale,
                0
            );
            ctx.matrixStack().translate(
                -width / 2,
                -font.lineHeight / 2.0f,
                0
            );
            font.drawInBatch(
                text.value(),
                0,
                0,
                text.color(),
                true,
                ctx.matrixStack().last().pose(),
                ctx.renderTypeBuffers(),
                Font.DisplayMode.NORMAL,
                0,
                15728880
            );
            ctx.matrixStack().popPose();
        }
        if (!texts.isEmpty()) {
            RenderSystem.disableCull();
            ctx.renderTypeBuffers().endLastBatch();
        }
    }

    @Override
    public void invalidateCache() {}

    @Override
    public void close() {}
}
