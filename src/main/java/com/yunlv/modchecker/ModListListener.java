package com.yunlv.modchecker;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import net.luckperms.api.model.user.User;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModListListener implements PluginMessageListener {

    private final ModChecker plugin;

    public ModListListener(ModChecker plugin) {
        this.plugin = plugin;
        // 注册常见 Mod 通信通道
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "minecraft:brand", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "fml:handshake", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "fabric:registry/sync", this);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, "fabric:custom_payload", this);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        // 只处理登录阶段或自定义通道
        if (channel.equals("minecraft:brand")) {
            // 解析客户端品牌，可能包含模组信息
            String brand = new String(message).toLowerCase();
            checkMods(player, brand);
        } else if (channel.equals("fml:handshake")) {
            // 解析 Forge 握手，提取 Mod 列表
            parseFmlHandshake(player, message);
        } else if (channel.startsWith("fabric:")) {
            // Fabric 通道，可能包含 Mod 标识
            String data = new String(message).toLowerCase();
            checkMods(player, data);
        }
    }

    private void parseFmlHandshake(Player player, byte[] message) {
        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            int discriminator = in.readByte();
            // 简化处理：直接转为字符串搜索关键词
            String raw = new String(message).toLowerCase();
            checkMods(player, raw);
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    private void checkMods(Player player, String rawData) {
        // LuckPerms 豁免检查
        if (plugin.isLuckPermsBypass() && plugin.getLuckPerms() != null) {
            User user = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());
            if (user != null && user.getCachedData().getPermissionData().checkPermission("modchecker.bypass").asBoolean()) {
                return;
            }
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
            if (plugin.getMode().equals("KICK")) {
                player.kickPlayer(plugin.getKickMessage().replace("%mods%", mods));
            } else {
                player.sendMessage(plugin.getWarnMessage().replace("%mods%", mods));
            }
        }
    }
}