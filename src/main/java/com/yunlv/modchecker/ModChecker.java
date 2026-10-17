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
import java.util.ArrayList;
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

        // 初始化 PacketEvents（由服务器插件提供）
        boolean packetActive = false;
        if (packetDetectionEnabled) {
            if (Bukkit.getPluginManager().getPlugin("PacketEvents") != null) {
                try {
                    PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
                    PacketEvents.getAPI().load();
                    PacketEvents.getAPI().init();

                    packetListener = new PacketListener(this);
                    PacketEvents.getAPI().getEventManager().registerListener(packetListener);

                    getLogger().info("PacketEvents 数据包检测已启用。");
                    packetActive = true;
                } catch (Throwable t) {
                    getLogger().warning("PacketEvents 初始化失败：" + t.getMessage());
                    t.printStackTrace();
                }
            } else {
                getLogger().warning("未找到 PacketEvents 插件，数据包检测不可用。");
            }
        }

        // 仅在 PacketEvents 不可用时注册回退监听器
        if (!packetActive) {
            new ModListListener(this);
            getLogger().info("已启用旧版通道监听器作为回退方案。");
        }

        // 注册命令
        ModCheckerCommand commandExecutor = new ModCheckerCommand(this);
        getCommand("modchecker").setExecutor(commandExecutor);
        getCommand("modchecker").setTabCompleter(commandExecutor);

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
     */
    public String getModeForPlayer(Player player) {
        if (hasPermission(player, "modchecker.bypass")) {
            return "BYPASS";
        }
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

    /**
     * 重载所有配置文件
     */
    public void reloadAllConfig() {
        loadConfig();
    }

    /**
     * 手动触发对指定玩家的检查请求
     */
    public void requestManualCheck(Player player) {
        player.sendPluginMessage(this, "minecraft:brand", new byte[0]);
        if (logToConsole) {
            getLogger().info("[手动检查] 已向玩家 " + player.getName() + " 发送检查请求。");
        }
    }

    /**
     * 添加黑名单模组
     */
    public void addBlacklistedMod(String mod) {
        blacklistedMods.add(mod.toLowerCase());
        saveBlacklist();
    }

    /**
     * 移除黑名单模组
     */
    public void removeBlacklistedMod(String mod) {
        blacklistedMods.remove(mod.toLowerCase());
        saveBlacklist();
    }

    /**
     * 保存黑名单到文件
     */
    private void saveBlacklist() {
        File blacklistFile = new File(getDataFolder(), "blacklist.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(blacklistFile);
        config.set("mods", new ArrayList<>(blacklistedMods));
        try {
            config.save(blacklistFile);
        } catch (Exception e) {
            getLogger().warning("保存黑名单失败：" + e.getMessage());
        }
    }

    /**
     * PacketEvents 是否已激活
     */
    public boolean isPacketDetectionActive() {
        return packetListener != null;
    }

    @Override
    public void onDisable() {
        if (PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().terminate();
        }
        getLogger().info("ModChecker 已禁用。");
    }
}