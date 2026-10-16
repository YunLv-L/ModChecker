package com.yunlv.modchecker;

import org.bukkit.ChatColor;

public class MessageUtil {

    private MessageUtil() {}

    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String replaceMods(String message, String mods) {
        return color(message.replace("%mods%", mods));
    }
}