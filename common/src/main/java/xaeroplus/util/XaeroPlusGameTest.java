package xaeroplus.util;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class XaeroPlusGameTest {
    public static void applyMixinsTest() {
        // forces all mixins to apply
        MixinEnvironment.getCurrentEnvironment().audit();
        Minecraft.getInstance().stop();
    }
}
