package com.yunlv.modchecker;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ModChecker extends JavaPlugin {

    private final Set<String> blacklistedMods = new HashSet<>();
    private String mode;
    private boolean luckPermsBypass;
    private String kickMessage;
    private String warnMessage;
    private LuckPerms luckPerms;

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

        // 注册监听器
        new ModListListener(this);

        getLogger().info("ModChecker 已启用，作者：YunLv");
    }

    private void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();
        mode = config.getString("mode", "KICK").toUpperCase();
        luckPermsBypass = config.getBoolean("luckperms-bypass", true);
        kickMessage = ChatColor.translateAlternateColorCodes('&', config.getString("kick-message", "&c你使用了违规模组：%mods%"));
        warnMessage = ChatColor.translateAlternateColorCodes('&', config.getString("warn-message", "&e检测到违规模组：%mods%"));

        // 加载黑名单
        File blacklistFile = new File(getDataFolder(), "blacklist.yml");
        YamlConfiguration blacklistConfig = YamlConfiguration.loadConfiguration(blacklistFile);
        blacklistedMods.clear();
        blacklistedMods.addAll(blacklistConfig.getStringList("mods"));
        getLogger().info("已加载 " + blacklistedMods.size() + " 个违规模组。");
    }

    public Set<String> getBlacklistedMods() {
        return blacklistedMods;
    }

    public String getMode() {
        return mode;
    }

    public boolean isLuckPermsBypass() {
        return luckPermsBypass;
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
        getLogger().info("ModChecker 已禁用。");
    }
}