package xaeroplus.util;

import org.spongepowered.asm.mixin.MixinEnvironment;

public class XaeroPlusGameTest {
    public static void applyMixinsTest() {
        // forces all mixins to apply
        MixinEnvironment.getCurrentEnvironment().audit();
        if (System.getenv("HMC_TEST") != null) {
            Runtime.getRuntime().exit(0);
        }
    }
}
