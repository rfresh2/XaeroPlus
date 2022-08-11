package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;
import xaero.map.gui.CursorBox;
import xaero.map.gui.GuiMap;
import xaero.map.gui.GuiTexturedButton;
import xaero.map.gui.IRightClickableElement;
import xaero.map.gui.ScreenBase;
import xaero.map.misc.Misc;

import java.io.IOException;
import java.util.function.Consumer;

@Mixin(value = GuiMap.class, remap = false)
public abstract class MixinGuiMap extends ScreenBase implements IRightClickableElement {

    GuiButton coordinateGotoButton;
    GuiTextField xTextEntryField;
    GuiTextField zTextEntryField;

    protected MixinGuiMap(GuiScreen parent, GuiScreen escape) {
        super(parent, escape);
    }

    @Shadow
    private static double destScale;

    @Shadow public abstract void addGuiButton(GuiButton b);

    @Shadow private double cameraX;

    @Shadow private double cameraZ;

    @Shadow public abstract void setFocused(GuiTextField field);

    @Inject(method = "changeZoom", at = @At(value = "HEAD"), cancellable = true)
    private void changeZoom(double factor, int zoomMethod, CallbackInfo ci) {
        // todo: restore ctrl zoom
        destScale *= Math.pow(1.2D, factor);
        ci.cancel();
    }

    @Inject(method = "initGui()V", at = @At(value = "TAIL"), remap = true)
    public void customInitGui(CallbackInfo ci) {
        int h = this.height / 2;
        coordinateGotoButton = new GuiTexturedButton(0, h + 40 , 20, 20, 229, 16, 16, 16, WorldMap.guiTextures, new Consumer<GuiButton>() {
            @Override
            public void accept(GuiButton guiButton) {
                onGotoCoordinatesButton(guiButton);
            }
        }, new CursorBox(new TextComponentString("Go to Coordinates")));
        addGuiButton(coordinateGotoButton);
        zTextEntryField = new GuiTextField(1, mc.fontRenderer, 2, h + 20, 50, 20);
        xTextEntryField = new GuiTextField(2, mc.fontRenderer, 2, h, 50, 20);
    }

    @Inject(method = "drawScreen(IIF)V", at = @At(value = "TAIL"), remap = true)
    public void customDrawScreen(int scaledMouseX, int scaledMouseY, float partialTicks, CallbackInfo ci) {

        if (mc.currentScreen != null && mc.currentScreen.getClass().equals(GuiMap.class)) {
            if (xTextEntryField.getText().isEmpty() && !this.xTextEntryField.isFocused()) {
                Misc.setFieldText(xTextEntryField, "X:", -11184811);
            }
            xTextEntryField.drawTextBox();
            if (zTextEntryField.getText().isEmpty() && !this.zTextEntryField.isFocused()) {
                Misc.setFieldText(zTextEntryField, "Z:", -11184811);
            }
            zTextEntryField.drawTextBox();
        }
    }

    @Inject(method = "mouseClicked(III)V", at = @At(value = "TAIL"), remap = true)
    public void mouseClicked(int x, int y, int button, CallbackInfo ci) throws IOException {
        if (button == 0) {
            handleTextFieldClick(x, y, xTextEntryField);
            handleTextFieldClick(x, y, zTextEntryField);
        }
    }

    private void handleTextFieldClick(int x, int y, GuiTextField guiTextField) {
        if ((x >= guiTextField.x && x <= guiTextField.x + guiTextField.width) && (y >= guiTextField.y && y <= guiTextField.y + xTextEntryField.height)) {
            guiTextField.setFocused(true);
            if (guiTextField.getText().startsWith("X:") || guiTextField.getText().startsWith("Z:")) {
                guiTextField.setText("");
                guiTextField.setTextColor(14737632);
            }
            this.setFocused(guiTextField);
            guiTextField.mouseClicked(x, y, 0);
        } else {
            guiTextField.setFocused(false);
        }
    }

    public void onGotoCoordinatesButton(final GuiButton b) {
        WorldMap.LOGGER.info("Go to coordinate button clicked!");
        try {
            int x = Integer.parseInt(xTextEntryField.getText());
            int z = Integer.parseInt(zTextEntryField.getText());
            cameraX = x;
            cameraZ = z;
        } catch (final NumberFormatException e) {
            // todo: do some default action if we detect placeholder text like go to 0,0?
            WorldMap.LOGGER.warn("Go to coordinates failed" , e);
        }
    }
}
