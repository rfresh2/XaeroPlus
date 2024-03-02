package xaeroplus.mixin.client.mc;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface AccessorWorldRenderer {
    @Accessor(value = "cullingFrustum")
    Frustum getFrustum();

    @Accessor(value = "viewArea")
    ViewArea getChunks();
}
