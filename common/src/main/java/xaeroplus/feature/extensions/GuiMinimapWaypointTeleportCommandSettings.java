package xaeroplus.feature.extensions;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.lib.client.graphics.util.TextureUtils;
import xaero.lib.client.render.util.GuiRenderUtil;
import xaeroplus.settings.SettingHooks;
import xaeroplus.settings.Settings;
import xaeroplus.settings.StringSetting;
import xaeroplus.util.ColorHelper;

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
        this.commandInput.setMaxLength(512);
        this.commandInput.setTextColor(ColorHelper.getColor(255, 255, 255, 255));
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
    public void renderBackground(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        TextureUtils.clearRenderTargetDepth(this.minecraft.getMainRenderTarget(), 1.0F);
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        GuiRenderUtil.flushGUI();
        super.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void render(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(
            this.font,
            this.title,
            this.width / 2,
            this.height / 2 - 42,
            ColorHelper.getColor(255, 255, 255, 255)
        );
        var formatKey = setting == Settings.REGISTRY.crossDimensionWaypointTeleportRotationFormat
            ? "xaeroplus.gui.cross_dimension_waypoint_teleport_rotation_format.description"
            : "xaeroplus.gui.cross_dimension_waypoint_teleport_format.description";
        guiGraphics.drawCenteredString(
            this.font,
            Component.translatable(formatKey),
            this.width / 2,
            this.height / 2 - 28,
            ColorHelper.getColor(160, 160, 160, 255)
        );
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
}
