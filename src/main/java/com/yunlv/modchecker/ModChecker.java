package com.yunlv.modchecker;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ModChecker extends JavaPlugin {

    private final Set<String> blacklistedMods = new HashSet<>();
    private String mode;
    private boolean luckPermsEnabled;
    private boolean packetDetectionEnabled;
    private boolean logToConsole;
    private String kickMessage;
    private String warnMessage;
    private LuckPerms luckPerms;
    private PacketListener packetListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("blacklist.yml", false);

        loadConfig();

        // 获取 LuckPerms API
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            luckPerms = LuckPermsProvider.get();
        } else {
            getLogger().warning("未找到 LuckPerms，权限节点豁免将不可用。");
        }

        // 初始化 PacketEvents
        if (packetDetectionEnabled && Bukkit.getPluginManager().getPlugin("PacketEvents") != null) {
            PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
            PacketEvents.getAPI().load();
            PacketEvents.getAPI().init();

            packetListener = new PacketListener(this);
            PacketEvents.getAPI().getEventManager().registerListener(packetListener);

            getLogger().info("PacketEvents 数据包检测已启用。");
        } else if (packetDetectionEnabled) {
            getLogger().warning("未找到 PacketEvents，数据包检测不可用。");
        }

        // 注册旧版通道监听器（作为回退方案）
        new ModListListener(this);

        getLogger().info("ModChecker 已启用，作者：YunLv");
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        mode = config.getString("mode", "KICK").toUpperCase();
        luckPermsEnabled = config.getBoolean("luckperms-enabled", true);
        packetDetectionEnabled = config.getBoolean("packet-detection", true);
        logToConsole = config.getBoolean("log-to-console", true);
        kickMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("kick-message", "&c你使用了违规模组：%mods%"));
        warnMessage = ChatColor.translateAlternateColorCodes('&',
                config.getString("warn-message", "&e检测到违规模组：%mods%"));

        File blacklistFile = new File(getDataFolder(), "blacklist.yml");
        YamlConfiguration blacklistConfig = YamlConfiguration.loadConfiguration(blacklistFile);
        blacklistedMods.clear();
        blacklistedMods.addAll(blacklistConfig.getStringList("mods"));
        getLogger().info("已加载 " + blacklistedMods.size() + " 个违规模组。");
    }

    /**
     * 检查玩家是否拥有指定权限节点
     * @param player 玩家
     * @param permission 权限节点
     * @return 是否拥有权限
     */
    public boolean hasPermission(Player player, String permission) {
        if (!luckPermsEnabled || luckPerms == null) {
            return player.hasPermission(permission);
        }
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return player.hasPermission(permission);
        }
        QueryOptions queryOptions = luckPerms.getContextManager().getQueryOptions(player);
        return user.getCachedData()
                .getPermissionData(queryOptions)
                .checkPermission(permission)
                .asBoolean();
    }

    /**
     * 根据玩家权限决定实际检测模式
     * @param player 玩家
     * @return KICK 或 WARN
     */
    public String getModeForPlayer(Player player) {
        // 拥有完全绕过权限的玩家直接跳过
        if (hasPermission(player, "modchecker.bypass")) {
            return "BYPASS";
        }
        // 拥有豁免踢出的玩家只警告
        if (hasPermission(player, "modchecker.exempt.kick")) {
            return "WARN";
        }
        return mode;
    }

    public Set<String> getBlacklistedMods() {
        return blacklistedMods;
    }

    public String getMode() {
        return mode;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public String getWarnMessage() {
        return warnMessage;
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
    }

    @Override
    public void onDisable() {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
        getLogger().info("ModChecker 已禁用。");
    }
}