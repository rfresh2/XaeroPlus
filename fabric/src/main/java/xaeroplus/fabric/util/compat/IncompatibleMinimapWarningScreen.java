package xaeroplus.fabric.util.compat;

import net.fabricmc.loader.api.Version;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public class IncompatibleMinimapWarningScreen extends Screen {

    private static Component getMessage(final Optional<Version> currentVersion, final Version compatibleMinimapVersion) {
        var msg = Component.empty();
        currentVersion.ifPresent(cv -> {
            msg
                .withStyle(ChatFormatting.RESET)
                .append(Component.translatable("xaeroplus.gui.minimap_incompatible.currently_installed_version"))
                .append(Component.literal(cv.getFriendlyString()).withStyle(ChatFormatting.RED))
                .append(Component.literal("\n"));
        });
        msg.append(
            Component.translatable("xaeroplus.gui.minimap_incompatible.required_version")
                .withStyle(ChatFormatting.RESET)
                .append(Component.literal(compatibleMinimapVersion.getFriendlyString()).withStyle(ChatFormatting.AQUA))
        );
        return msg;
    }

    private final Component titleComponent;
    private final Component messageComponent;
    private MultiLineLabel message = MultiLineLabel.EMPTY;


    public IncompatibleMinimapWarningScreen(Optional<Version> currentVersion, final Version compatibleMinimapVersion) {
        super(Component.literal("XaeroPlus"));
        titleComponent = Component.translatable("xaeroplus.gui.minimap_incompatible.title").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
        messageComponent = getMessage(currentVersion, compatibleMinimapVersion);
    }

    @Override
    public void init() {
        this.message = MultiLineLabel.create(this.font, this.messageComponent, this.width - 100);
        addRenderableWidget(
            Button.builder(Component.translatable("xaeroplus.gui.minimap_incompatible.download_minimap"), button -> {
                    Util.getPlatform().openUri("https://modrinth.com/mod/xaeros-minimap/versions");
                    Minecraft.getInstance().close();
            })
            .bounds(width / 2 - 100 - 75, 150, 150, 20)
            .build()
        );

        addRenderableWidget(
            Button.builder(Component.translatable("xaeroplus.gui.minimap_incompatible.exit"), button -> {
                    Minecraft.getInstance().close();
                })
                .bounds(width / 2 + 100 - 75, 150, 150, 20)
                .build()
        );
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, titleComponent, this.width / 2, 50, 16777215);
        int i = this.width / 2 - this.message.getWidth() / 2;
        this.message.renderLeftAligned(guiGraphics, i, 75, 18, 16777215);
    }
}
