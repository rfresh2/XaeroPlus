package xaeroplus.util;

import org.spongepowered.asm.mixin.MixinEnvironment;

public class XaeroPlusGameTest {
    public static void applyMixinsTest() {
        // forces all mixins to apply
        try {
            // resolve classload order issue
            // There's a circular dependency between MapShaders and CustomRenderTypes
            // CustomRenderTypes needs to load first, which then in its <clinit> loads MapShaders after initializing a particular static field
            Class.forName(xaero.map.graphics.CustomRenderTypes.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        MixinEnvironment.getCurrentEnvironment().audit();
    }
}
