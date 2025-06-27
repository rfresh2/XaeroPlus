package xaeroplus.commands;

import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public interface XPClientCommandSource extends SharedSuggestionProvider {
    void xaeroplus$sendSuccess(Component message);

    void xaeroplus$sendFailure(Component message);
}
