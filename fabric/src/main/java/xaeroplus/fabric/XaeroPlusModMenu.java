package xaeroplus.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import xaero.lib.client.gui.config.context.BuiltInEditConfigScreenContexts;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;

public class XaeroPlusModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> new GuiXaeroPlusWorldMapSettings(new GuiWorldMapSettings(BuiltInEditConfigScreenContexts.CLIENT), screen);
    }
}
