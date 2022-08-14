package xaeroplus.mixin.client;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.TextComponentString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xaero.map.WorldMap;
import xaero.map.animation.SlowingAnimation;
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

    // todo: make this gui a popout widget like waypoints
    GuiButton coordinateGotoButton;
    GuiTextField xTextEntryField;
    GuiTextField zTextEntryField;

    protected MixinGuiMap(GuiScreen parent, GuiScreen escape) {
        super(parent, escape);
    }

    @Shadow
    private static double destScale;
    @Shadow
    public abstract void addGuiButton(GuiButton b);
    @Shadow
    private double cameraX;
    @Shadow
    private double cameraZ;
    @Shadow
    private int lastZoomMethod;
    @Shadow
    private SlowingAnimation cameraDestinationAnimX;
    @Shadow
    private SlowingAnimation cameraDestinationAnimZ;
    @Shadow
    public abstract void setFocused(GuiTextField field);
    @Shadow
    protected abstract void closeDropdowns();

    /**
     * @author rfresh2
     * @reason Unlock minimum zoom level on WorldMap GUI
     */
    @Overwrite
    private void changeZoom(double factor, int zoomMethod) {
        this.closeDropdowns();
        this.lastZoomMethod = zoomMethod;
        this.cameraDestinationAnimX = null;
        this.cameraDestinationAnimZ = null;
        if (GuiMap.isCtrlKeyDown()) {
            double destScaleBefore = destScale;
            if (destScale >= 1.0) {
                if (factor > 0.0) {
                    destScale = Math.ceil(destScale);
                } else {
                    destScale = Math.floor(destScale);
                }

                if (destScaleBefore == destScale) {
                    destScale += factor > 0.0 ? 1.0 : -1.0;
                }

                if (destScale == 0.0) {
                    destScale = 0.5;
                }
            } else {
                double reversedScale = 1.0 / destScale;
                double log2 = Math.log(reversedScale) / Math.log(2.0);
                if (factor > 0.0) {
                    log2 = Math.floor(log2);
                } else {
                    log2 = Math.ceil(log2);
                }

                destScale = 1.0 / Math.pow(2.0, log2);
                if (destScaleBefore == destScale) {
                    destScale = 1.0 / Math.pow(2.0, log2 + (double)(factor > 0.0 ? -1 : 1));
                }
            }
        } else {
            destScale *= Math.pow(1.2, factor);
        }

        if (destScale < 0.0625) {
            // remove min zoom
//            destScale = 0.0625;
        } else if (destScale > 50.0) {
            destScale = 50.0;
        }
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
