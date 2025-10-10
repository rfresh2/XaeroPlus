package xaeroplus.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class NotificationUtil {
    public static void inGameNotification(String message) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.getChat().addMessage(Component.literal("[XaeroPlus] ").append(Component.literal(message)));
        }
    }

    public static void errorNotification(String message) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.getChat().addMessage(Component
                .literal("[XaeroPlus] ")
                .withStyle(ChatFormatting.RED)
                .append(Component
                    .literal(message)
                    .withStyle(ChatFormatting.WHITE)));
        }
    }
}
