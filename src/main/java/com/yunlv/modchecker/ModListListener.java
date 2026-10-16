package com.yunlv.modchecker;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 旧版通道监听器（作为 PacketEvents 未安装时的回退方案）
 */
public class ModListListener implements PluginMessageListener {

    private final ModChecker plugin;

    public ModListListener(ModChecker plugin) {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "fml:handshake", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "fabric:registry/sync", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "fabric:custom_payload", this);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (channel.equals("minecraft:brand")) {
            String brand = new String(message).toLowerCase();
            checkMods(player, brand);
        } else if (channel.equals("fml:handshake")) {
            parseFmlHandshake(player, message);
        } else if (channel.startsWith("fabric:")) {
            String data = new String(message).toLowerCase();
            checkMods(player, data);
        }
    }

    private void parseFmlHandshake(Player player, byte[] message) {
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            in.readByte();
            String raw = new String(message).toLowerCase();
            checkMods(player, raw);
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    private void checkMods(Player player, String rawData) {
        if (plugin.hasPermission(player, "modchecker.bypass")) {
            return;
        }

        Set<String> blacklist = plugin.getBlacklistedMods();
        List<String> found = new ArrayList<>();

        for (String mod : blacklist) {
            if (rawData.contains(mod.toLowerCase())) {
                found.add(mod);
            }
        }

        if (!found.isEmpty()) {
            String mods = String.join(", ", found);
            String effectiveMode = plugin.getModeForPlayer(player);
            if ("KICK".equals(effectiveMode)) {
                player.kickPlayer(plugin.getKickMessage().replace("%mods%", mods));
            } else if (!"BYPASS".equals(effectiveMode)) {
                player.sendMessage(plugin.getWarnMessage().replace("%mods%", mods));
            }
        }
    }
}