package xaeroplus.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;

import static net.minecraft.network.chat.Component.literal;

public class NotificationUtil {
    public static void inGameNotification(String message) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.gui.hud.getChat().addClientSystemMessage(
                    literal("[").withStyle(ChatFormatting.GRAY)
                        .append(literal("XaeroPlus").withStyle(ChatFormatting.AQUA))
                        .append(literal("] ").withStyle(ChatFormatting.GRAY))
                        .append(literal(message).withStyle(ChatFormatting.WHITE))
                );
            }
        });
    }

    public static void errorNotification(String message) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null) {
                mc.gui.hud.getChat().addClientSystemMessage(
                    literal("[").withStyle(ChatFormatting.GRAY)
                        .append(literal("XaeroPlus").withStyle(ChatFormatting.RED))
                        .append(literal("] ").withStyle(ChatFormatting.GRAY))
                        .append(literal(message).withStyle(ChatFormatting.RED))
                );
            }
        });
    }
}
