package xaeroplus.neo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import xaero.map.gui.GuiWorldMapSettings;
import xaeroplus.feature.extensions.GuiXaeroPlusWorldMapSettings;

public class XaeroPlusConfigScreenFactory implements IConfigScreenFactory {
    @Override
    public Screen createScreen(final Minecraft mc, final Screen screen) {
        return new GuiXaeroPlusWorldMapSettings(new GuiWorldMapSettings(screen), screen);
    }
}
