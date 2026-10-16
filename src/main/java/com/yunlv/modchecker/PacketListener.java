package com.yunlv.modchecker;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import org.bukkit.entity.Player;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PacketListener extends PacketListenerAbstract {

    private final ModChecker plugin;

    public PacketListener(ModChecker plugin) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE) {
            return;
        }

        WrapperPlayClientPluginMessage packet = new WrapperPlayClientPluginMessage(event);
        String channel = packet.getChannelName();
        byte[] data = packet.getData();
        Player player = event.getPlayer();

        if (player == null) return;

        // Forge FML 握手 - 解析 ModList 数据包
        if (channel.equals("fml:handshake")) {
            parseFmlHandshake(player, data);
        }

        // 客户端品牌信息
        if (channel.equals("minecraft:brand")) {
            String brand = new String(data, StandardCharsets.UTF_8).toLowerCase();
            checkMods(player, brand, "客户端品牌");
        }

        // Fabric 通道检测
        if (channel.startsWith("fabric:")) {
            String raw = new String(data, StandardCharsets.UTF_8).toLowerCase();
            checkMods(player, raw, "Fabric 通道");
        }
    }

    /**
     * 解析 Forge FML 握手 ModList 数据包
     * FML 握手协议中 discriminator 为 2 表示 ModList 包
     */
    private void parseFmlHandshake(Player player, byte[] data) {
        try {
            if (data.length < 1) return;
            int discriminator = data[0];
            // discriminator == 2 表示 ModList 包
            if (discriminator != 2) return;

            // 将原始数据转为字符串进行关键词匹配
            String raw = new String(data, StandardCharsets.UTF_8).toLowerCase();
            checkMods(player, raw, "Forge FML 握手");
        } catch (Exception e) {
            // 忽略解析错误
        }
    }

    /**
     * 检查原始数据中是否包含黑名单模组
     */
    private void checkMods(Player player, String rawData, String source) {
        Set<String> blacklist = plugin.getBlacklistedMods();
        List<String> found = new ArrayList<>();

        for (String mod : blacklist) {
            if (rawData.contains(mod.toLowerCase())) {
                found.add(mod);
            }
        }

        if (found.isEmpty()) return;

        String mods = String.join(", ", found);

        // 根据玩家权限决定处理方式
        String effectiveMode = plugin.getModeForPlayer(player);

        if (plugin.isLogToConsole()) {
            plugin.getLogger().warning("[检测] 玩家 " + player.getName()
                    + " 使用违规模组: " + mods + " (来源: " + source + ")");
        }

        switch (effectiveMode) {
            case "BYPASS":
                return;
            case "KICK":
                player.kickPlayer(plugin.getKickMessage().replace("%mods%", mods));
                break;
            case "WARN":
            default:
                player.sendMessage(plugin.getWarnMessage().replace("%mods%", mods));
                break;
        }
    }
}