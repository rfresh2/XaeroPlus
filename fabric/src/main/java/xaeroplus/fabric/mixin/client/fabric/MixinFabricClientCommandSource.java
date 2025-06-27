package xaeroplus.fabric.mixin.client.fabric;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import xaeroplus.commands.XPClientCommandSource;

@Mixin(FabricClientCommandSource.class)
public interface MixinFabricClientCommandSource extends XPClientCommandSource {
    @Override
    default void xaeroplus$sendSuccess(Component message) {
        ((FabricClientCommandSource) this).sendFeedback(message);
    }

    @Override
    default void xaeroplus$sendFailure(Component message) {
        ((FabricClientCommandSource) this).sendError(message);
    }
}
