package xaeroplus.forge.mixin.client;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import xaeroplus.commands.XPClientCommandSource;

@Mixin(CommandSourceStack.class)
public abstract class MixinCommandSourceStack implements XPClientCommandSource {

    @Override
    public void xaeroplus$sendSuccess(Component message) {
        ((CommandSourceStack) (Object) this).sendSuccess(() -> message, false);
    }

    @Override
    public void xaeroplus$sendFailure(Component message) {
        ((CommandSourceStack) (Object) this).sendFailure(message);
    }
}
