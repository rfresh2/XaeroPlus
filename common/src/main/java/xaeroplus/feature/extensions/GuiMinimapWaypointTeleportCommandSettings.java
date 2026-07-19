package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.SettingHooks;
import xaeroplus.settings.Settings;

public class GuiMinimapWaypointTeleportCommandSettings extends Screen {
    private final Screen parent;
    private EditBox commandInput;
    private Button saveButton;

    public GuiMinimapWaypointTeleportCommandSettings(final Screen parent, final Screen escapeScreen) {
        super(Component.translatable("xaeroplus.gui.minimap_waypoint_teleport_command.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.commandInput = new EditBox(
            this.font,
            this.width / 2 - 150,
            this.height / 2 - 10,
            300,
            20,
            Component.translatable("xaeroplus.gui.minimap_waypoint_teleport_command.input")
        );
        this.commandInput.setMaxLength(256);
        this.commandInput.setValue(Settings.REGISTRY.minimapWaypointCustomTeleportCommand.get());
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
            Component.translatable("xaeroplus.gui.minimap_waypoint_teleport_command.description"),
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
        Settings.REGISTRY.minimapWaypointCustomTeleportCommand.setValue(command);
        SettingHooks.saveSettings();
        onClose();
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry() {
        return new ScreenSwitchSettingEntry(
            "xaeroplus.gui.minimap_waypoint_teleport_command.settings",
            GuiMinimapWaypointTeleportCommandSettings::new,
            null,
            true
        );
    }
}
