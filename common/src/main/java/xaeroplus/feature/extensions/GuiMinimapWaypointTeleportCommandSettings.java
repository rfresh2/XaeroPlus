package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.SettingHooks;
import xaeroplus.settings.Settings;
import xaeroplus.settings.StringSetting;

public class GuiMinimapWaypointTeleportCommandSettings extends Screen {
    private final Screen parent;
    private final StringSetting setting;
    private EditBox commandInput;
    private Button saveButton;

    public GuiMinimapWaypointTeleportCommandSettings(final Screen parent, final Screen escapeScreen, final StringSetting setting) {
        super(Component.translatable(setting.getSettingNameTranslationKey()));
        this.parent = parent;
        this.setting = setting;
    }

    @Override
    protected void init() {
        this.commandInput = new EditBox(
            this.font,
            this.width / 2 - 150,
            this.height / 2 - 10,
            300,
            20,
            Component.translatable("xaeroplus.gui.cross_dimension_waypoint_teleport_format.input")
        );
        this.commandInput.setMaxLength(256);
        this.commandInput.setValue(this.setting.get());
        this.commandInput.setResponder(value -> this.saveButton.active = !value.isBlank());
        this.addRenderableWidget(this.commandInput);
        this.saveButton = this.addRenderableWidget(
            Button.builder(Component.translatable("gui.done"), button -> save())
                .bounds(this.width / 2 - 104, this.height / 2 + 26, 100, 20)
                .build()
        );
        this.saveButton.active = !this.commandInput.getValue().isBlank();
        this.addRenderableWidget(
            Button.builder(Component.translatable("gui.cancel"), button -> onClose())
                .bounds(this.width / 2 + 4, this.height / 2 + 26, 100, 20)
                .build()
        );
        this.setInitialFocus(this.commandInput);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 42, 0xFFFFFF);
        guiGraphics.drawCenteredString(
            this.font,
            Component.translatable("xaeroplus.gui.cross_dimension_waypoint_teleport_format.description"),
            this.width / 2,
            this.height / 2 - 28,
            0xA0A0A0
        );
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    private void save() {
        String command = this.commandInput.getValue();
        if (command.isBlank()) return;
        this.setting.setValue(command);
        SettingHooks.saveSettings();
        onClose();
    }

    public static ScreenSwitchSettingEntry[] getScreenSwitchSettingEntries() {
        return new ScreenSwitchSettingEntry[] {
            getScreenSwitchSettingEntry(Settings.REGISTRY.crossDimensionWaypointTeleportFormat),
            getScreenSwitchSettingEntry(Settings.REGISTRY.crossDimensionWaypointTeleportRotationFormat)
        };
    }

    private static ScreenSwitchSettingEntry getScreenSwitchSettingEntry(final StringSetting setting) {
        return new ScreenSwitchSettingEntry(
            setting.getSettingNameTranslationKey(),
            (parent, escapeScreen) -> new GuiMinimapWaypointTeleportCommandSettings(parent, escapeScreen, setting),
            null,
            true
        );
    }
}
