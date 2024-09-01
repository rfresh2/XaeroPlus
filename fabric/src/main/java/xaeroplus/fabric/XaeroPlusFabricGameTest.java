package xaeroplus.fabric;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

public class XaeroPlusFabricGameTest implements FabricGameTest {
    @GameTest(template = EMPTY_STRUCTURE)
    public void waitForJitCompiler(GameTestHelper context) {
        // todo: we need to actually load more chunks and ensure xaero's writes chunk data to file
        //      as is, the hmc test just stands in place in an empty structure
        //      most xaero methods aren't actually getting jit compiled no matter how long we wait here
        //      also there's probably some JVM args that could be used to force c2 jit to compile earlier

        context.startSequence()
            .thenIdle(20 * 60 * 10) // 10 minutes
            .thenSucceed();
    }
}
