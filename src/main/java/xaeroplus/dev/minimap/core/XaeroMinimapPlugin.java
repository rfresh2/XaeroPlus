package xaeroplus.dev.minimap.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.MCVersion;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;

import java.util.Map;

@MCVersion("1.12.2")
@TransformerExclusions({"xaero.common.core.transformer", "xaero.common.core"})
public class XaeroMinimapPlugin implements IFMLLoadingPlugin {
   public String[] getASMTransformerClass() {
      return new String[]{
         "xaero.common.core.transformer.ChunkTransformer",
         "xaero.common.core.transformer.NetHandlerPlayClientTransformer",
         "xaero.common.core.transformer.EntityPlayerTransformer",
         "xaero.common.core.transformer.AbstractClientPlayerTransformer",
         "xaero.common.core.transformer.WorldClientTransformer",
         "xaero.common.core.transformer.EntityPlayerSPTransformer",
         "xaero.common.core.transformer.PlayerListTransformer",
         "xaero.common.core.transformer.SaveFormatTransformer",
         "xaero.common.core.transformer.GuiIngameForgeTransformer",
         "xaero.common.core.transformer.GuiBossOverlayTransformer",
         "xaero.common.core.transformer.ModelRendererTransformer"
      };
   }

   public String getModContainerClass() {
      return "xaero.common.core.CoreModContainer";
   }

   public String getSetupClass() {
      return null;
   }

   public void injectData(Map<String, Object> data) {
   }

   public String getAccessTransformerClass() {
      return null;
   }
}
