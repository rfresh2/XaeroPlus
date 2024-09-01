package xaeroplus.fabric;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class XaeroPlusFabricGameTest implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void waitForJitCompiler(GameTestHelper context) {
        context.startSequence()
            .thenIdle(20 * 60 * 10) // 10 minutes
            .thenSucceed();
    }
}
