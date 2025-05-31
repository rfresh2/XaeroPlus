package xaeroplus.feature.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.settings.Settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class DrawOrderScreen extends Screen {
    private List<DrawFeatureEntry> entries = new ArrayList<>();
    private Screen parent;
    public DrawOrderScreen(Screen parent, Screen escapeScreen) {
        super(Component.literal("XaeroPlus Draw Order"));
        this.parent = parent;
    }

    @Override
    public void init() {
        this.entries = loadEntries();
        for (DrawFeatureEntry entry : entries) {
            addRenderableWidget(entry);
        }
        Minecraft mc = Minecraft.getInstance();
        var exitButton = Button.builder(Component.literal("Exit"), (button) -> {
            mc.setScreen(parent);
        }).pos((mc.screen.width / 2) - 40, mc.screen.height - 25).size(80, 22).build();
        addRenderableWidget(exitButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {
        this.renderDirtBackground(guiGraphics);
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry() {
        return new ScreenSwitchSettingEntry(
            "Draw Order",
            DrawOrderScreen::new,
            null,
            true
        );
    }

    public List<DrawFeatureEntry> loadEntries() {
        List<DrawFeatureEntry> entries = new ArrayList<>();
        var list = Arrays.asList(Settings.REGISTRY.drawOrderSetting.getSerializedValue().split(","));
        for (int i = 0; i < list.size(); i++) {
            String featureId = list.get(i);
            var entry = new DrawFeatureEntry(featureId, i, this);
            entries.add(entry);
        }
        return entries;
    }

    public void saveEntries(List<DrawFeatureEntry> entries) {
        entries.sort(Comparator.comparingInt(e -> e.priority));
        List<String> sortedIds = new ArrayList<>();
        for (var e : entries) {
            sortedIds.add(e.id);
        }
        var serialized = String.join(",", sortedIds);
        Settings.REGISTRY.drawOrderSetting.setValue(serialized);
        rebuildWidgets();
    }

    public static class DrawFeatureEntry extends AbstractWidget {
        public String id;
        public int priority;
        static final String up = "↑";
        static final String down = "↓";
        private final DrawOrderScreen drawOrderScreen;

        public DrawFeatureEntry(String id, int priority, DrawOrderScreen drawOrderScreen) {
            super(50, 20 + (priority * 22), Minecraft.getInstance().screen.width, 25, Component.literal(id));
            this.id = id;
            this.priority = priority;
            this.drawOrderScreen = drawOrderScreen;
        }

        @Override
        protected void renderWidget(final GuiGraphics guiGraphics, final int mouseX, final int mouseY, final float partialTick) {
            var mc = Minecraft.getInstance();
            var font = mc.font;
            guiGraphics.drawString(font, id, getX() + 50, getY(), -1);
            guiGraphics.drawCenteredString(font, up, getX() + 12, getY(), upHovered(mouseX, mouseY) ? -1 : -5592406);
            guiGraphics.drawCenteredString(font, down, getX() + 35, getY(), downHovered(mouseX, mouseY) ? -1 : -5592406);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            if (upHovered((int) mouseX, (int) mouseY)) {
                int newPriority = this.priority - 1;
                if (newPriority >= 0) {
                    var toSwapWithEntry = drawOrderScreen.entries.get(newPriority);
                    this.priority = newPriority;
                    toSwapWithEntry.priority = toSwapWithEntry.priority + 1;
                    drawOrderScreen.entries.set(newPriority, this);
                    drawOrderScreen.entries.set(priority, toSwapWithEntry);
                    drawOrderScreen.saveEntries(drawOrderScreen.entries);
                }
            } else if (downHovered((int) mouseX, (int) mouseY)) {
                int newPriority = this.priority + 1;
                if (newPriority < drawOrderScreen.entries.size()) {
                    var toSwapWithEntry = drawOrderScreen.entries.get(newPriority);
                    this.priority = newPriority;
                    toSwapWithEntry.priority = toSwapWithEntry.priority - 1;
                    drawOrderScreen.entries.set(newPriority, this);
                    drawOrderScreen.entries.set(priority, toSwapWithEntry);
                    drawOrderScreen.saveEntries(drawOrderScreen.entries);
                }
            }
        }

        private boolean upHovered(int mouseX, int mouseY) {
            return mouseX >= getX() + 2 && mouseX <= getX() + 22 && mouseY >= getY() - 8 && mouseY <= getY() + 8;
        }

        private boolean downHovered(int mouseX, int mouseY) {
            return mouseX >= getX() + 25 && mouseX <= getX() + 55 && mouseY >= getY() - 8 && mouseY <= getY() + 8;
        }

        @Override
        protected void updateWidgetNarration(final NarrationElementOutput narrationElementOutput) {}
    }
}
