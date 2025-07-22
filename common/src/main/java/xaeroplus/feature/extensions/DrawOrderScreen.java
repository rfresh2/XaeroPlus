package xaeroplus.feature.extensions;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import xaero.map.gui.ScreenSwitchSettingEntry;
import xaeroplus.module.impl.TickTaskExecutor;
import xaeroplus.settings.Settings;
import xaeroplus.util.ColorHelper;
import xaeroplus.util.DrawOrderHelper;

import java.util.ArrayList;
import java.util.List;

public class DrawOrderScreen extends Screen {
    static Minecraft mc = Minecraft.getInstance();
    Screen parent;
    DrawFeatureList drawFeatureList;
    List<String> drawFeatureIdOrder;
    int selected;

    public DrawOrderScreen(Screen parent, Screen escapeScreen) {
        super(Component.translatable("xaeroplus.gui.draw_order.title"));
        this.parent = parent;
        this.drawFeatureIdOrder = new ArrayList<>();
        this.selected = -1;
    }

    @Override
    public void init() {
        drawFeatureIdOrder = loadEntries();
        drawFeatureList = new DrawFeatureList(this);
        addWidget(drawFeatureList);
        addRenderableWidget(
            Button.builder(Component.translatable("gui.done"),
                b -> mc.setScreen(parent))
                .bounds(this.width / 2 - 100, this.height - 34, 200, 20)
                .build()
        );
        addRenderableWidget(
            Button.builder(Component.translatable("xaeroplus.gui.draw_order.reset"),
                b -> TickTaskExecutor.INSTANCE.execute(() -> {
                    Settings.REGISTRY.drawOrderSetting.setValue("");
                    init(mc, width, height);
                }))
                .bounds(this.width - 82, 2, 80, 20)
                .build()
        );
        if (!drawFeatureIdOrder.isEmpty()) {
            selected = 0;
            drawFeatureList.setFocused(drawFeatureList.getFirstElement());
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (drawFeatureList != null) {
            drawFeatureList.releaseDrag();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        drawFeatureList.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(mc.font, title, width / 2, 5, -1);
        guiGraphics.drawCenteredString(mc.font, Component.translatable("xaeroplus.gui.draw_order.subtitle"), width / 2, height - 52, -1);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    public List<String> loadEntries() {
        return DrawOrderHelper.load();
    }

    public void saveEntries(List<String> entries) {
        Settings.REGISTRY.drawOrderSetting.setValue(DrawOrderHelper.serialize(entries));
    }

    public static ScreenSwitchSettingEntry getScreenSwitchSettingEntry() {
        return new ScreenSwitchSettingEntry(
            "Draw Order",
            DrawOrderScreen::new,
            null,
            true
        );
    }

    public static class DrawFeatureList extends ObjectSelectionList<DrawFeatureEntry> {
        boolean dragging;
        int dragStartX;
        int dragStartY;
        int dragged;
        int draggedOffsetX;
        int draggedOffsetY;
        DrawOrderScreen drawOrderScreen;

        public DrawFeatureList(DrawOrderScreen drawOrderScreen) {
            super(mc, drawOrderScreen.width, drawOrderScreen.height, 30, drawOrderScreen.height - 61, 24);
            this.drawOrderScreen = drawOrderScreen;
            this.dragged = -1;
            createEntries().forEach(this::addEntry);
            if (drawOrderScreen.selected != -1) {
                setFocused(getEntry(drawOrderScreen.selected));
            }
        }

        @Override
        public boolean isFocused() {
            return drawOrderScreen.getFocused() == this;
        }

        @Override
        public void setFocused(GuiEventListener guiEventListener) {
            if (guiEventListener instanceof DrawFeatureEntry entry) {
                drawOrderScreen.selected = entry.index;
            }
            if (guiEventListener == null) {
                drawOrderScreen.selected = -1;
            }
            super.setFocused(guiEventListener);
            if (getFocused() == null) {
                setSelected(null);
            }
        }

        List<DrawFeatureEntry> createEntries() {
            List<DrawFeatureEntry> entries = new ArrayList<>();
            for (int i = 0; i < drawOrderScreen.drawFeatureIdOrder.size(); i++) {
                var entry = new DrawFeatureEntry(drawOrderScreen, this, i);
                entries.add(entry);
            }
            return entries;
        }

        void releaseDrag() {
            dragging = false;
            dragged = -1;
            drawOrderScreen.saveEntries(drawOrderScreen.drawFeatureIdOrder);
        }

        @Override
        public int getScrollbarPosition() {
            return (this.width / 2) + 164;
        }

        @Override
        public int getRowWidth() {
            return 300;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            this.setRenderBackground(false);
            this.setRenderTopAndBottom(false);
            renderBackdrop(guiGraphics);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            if (dragging) {
                var draggedEntry = getEntry(dragged);
                draggedEntry.renderEntryText(guiGraphics, mouseX + draggedOffsetX, mouseY + draggedOffsetY);
                var hoveredEntry = getEntryAtPosition(mouseX, mouseY);
                int hoveredIndex = hoveredEntry == null ? -1 : hoveredEntry.index;
                if (hoveredIndex != -1 && hoveredIndex != dragged) {
                    String draggedId = drawOrderScreen.drawFeatureIdOrder.get(dragged);
                    int slideDirection = hoveredIndex < dragged ? 1 : -1;
                    for (int i = dragged; i != hoveredIndex; i -= slideDirection) {
                        drawOrderScreen.drawFeatureIdOrder.set(i, drawOrderScreen.drawFeatureIdOrder.get(i - slideDirection));
                    }
                    drawOrderScreen.drawFeatureIdOrder.set(hoveredIndex, draggedId);
                    dragged = hoveredIndex;
                }
            } else if (dragged != -1 && (Math.abs(mouseX - dragStartX) > 5 || Math.abs(mouseY - dragStartY) > 5)) {
                dragging = true;
                setFocused(null);
            }
        }

        public void renderBackdrop(GuiGraphics guiGraphics) {
            guiGraphics.fill(0, 0, width, height, ColorHelper.getColor(0, 0, 0, 100));
        }
    }

    public static class DrawFeatureEntry extends ObjectSelectionList.Entry<DrawFeatureEntry> {
        DrawOrderScreen drawOrderScreen;
        DrawFeatureList drawFeatureList;
        int index;
        int lastRenderX;
        int lastRenderY;
        int lastMouseX;
        int lastMouseY;

        public DrawFeatureEntry(DrawOrderScreen drawOrderScreen, DrawFeatureList drawFeatureList, int index) {
            this.drawOrderScreen = drawOrderScreen;
            this.drawFeatureList = drawFeatureList;
            this.index = index;
        }

        public void renderEntryText(GuiGraphics guiGraphics, int x, int y) {
            String id = drawOrderScreen.drawFeatureIdOrder.get(index);
            guiGraphics.drawString(mc.font, id, x + 6, y + 6, -1);
        }

        @Override
        public Component getNarration() {
            return Component.empty();
        }

        @Override
        public void render(final GuiGraphics guiGraphics, final int index, final int y, final int x, final int width, final int height, final int mouseX, final int mouseY, final boolean hovering, final float partialTick) {
            lastRenderX = x;
            lastRenderY = y;
            lastMouseX = mouseX;
            lastMouseY = mouseY;
            if (!drawFeatureList.dragging || drawFeatureList.dragged != index) {
                renderEntryText(guiGraphics, x, y);
            }
        }

        @Override
        public void renderBack(
            GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean isMouseOver, float partialTick
        ) {
            if (drawFeatureList.getSelected() != this) {
                guiGraphics.fill(left - 2, top, left - 2 + width, top + height, ColorHelper.getColor(0, 0, 0, 150));
                guiGraphics.renderOutline(left - 2, top, width, height, ColorHelper.getColor(68, 68, 68, 255));
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                drawFeatureList.dragging = false;
                drawFeatureList.dragged = index;
                drawFeatureList.draggedOffsetX = (int) (lastRenderX - mouseX);
                drawFeatureList.draggedOffsetY = (int) (lastRenderY - mouseY);
                drawFeatureList.dragStartX = (int) mouseX;
                drawFeatureList.dragStartY = (int) mouseY;
                if (drawFeatureList.getSelected() != this) {
                    return true;
                }
                drawFeatureList.setFocused(null);
            } else {
                drawFeatureList.setFocused(null);
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public void mouseMoved(double mouseX, double mouseY) {
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            super.mouseMoved(mouseX, mouseY);
        }

        @Override
        public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }
}
