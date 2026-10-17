package com.yunlv.modchecker;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModCheckerCommand implements CommandExecutor, TabCompleter {

    private final ModChecker plugin;

    public ModCheckerCommand(ModChecker plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload":
                if (!hasPerm(sender, "modchecker.command.reload")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此操作。");
                    return true;
                }
                plugin.reloadAllConfig();
                sender.sendMessage(ChatColor.GREEN + "[ModChecker] 配置已重载，当前黑名单数量："
                        + plugin.getBlacklistedMods().size());
                return true;

            case "check":
                if (!hasPerm(sender, "modchecker.command.check")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此操作。");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法：/modchecker check <玩家名>");
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "未找到在线玩家：" + args[1]);
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "[ModChecker] 正在检查玩家 " + target.getName() + " ...");
                plugin.requestManualCheck(target);
                sender.sendMessage(ChatColor.GREEN + "[ModChecker] 已向客户端发送检查请求，请查看控制台日志。");
                return true;

            case "list":
                if (!hasPerm(sender, "modchecker.command.list")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此操作。");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "===== 违规模组黑名单（共 "
                        + plugin.getBlacklistedMods().size() + " 个）=====");
                for (String mod : plugin.getBlacklistedMods()) {
                    sender.sendMessage(ChatColor.GRAY + " - " + ChatColor.WHITE + mod);
                }
                return true;

            case "add":
                if (!hasPerm(sender, "modchecker.command.add")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此操作。");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法：/modchecker add <模组ID>");
                    return true;
                }
                String toAdd = args[1].toLowerCase();
                if (plugin.getBlacklistedMods().contains(toAdd)) {
                    sender.sendMessage(ChatColor.YELLOW + "该模组已在黑名单中：" + toAdd);
                    return true;
                }
                plugin.addBlacklistedMod(toAdd);
                sender.sendMessage(ChatColor.GREEN + "[ModChecker] 已添加：" + toAdd);
                return true;

            case "remove":
                if (!hasPerm(sender, "modchecker.command.remove")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此操作。");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "用法：/modchecker remove <模组ID>");
                    return true;
                }
                String toRemove = args[1].toLowerCase();
                if (!plugin.getBlacklistedMods().contains(toRemove)) {
                    sender.sendMessage(ChatColor.YELLOW + "该模组不在黑名单中：" + toRemove);
                    return true;
                }
                plugin.removeBlacklistedMod(toRemove);
                sender.sendMessage(ChatColor.GREEN + "[ModChecker] 已移除：" + toRemove);
                return true;

            case "status":
                if (!hasPerm(sender, "modchecker.command.status")) {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此操作。");
                    return true;
                }
                sender.sendMessage(ChatColor.GOLD + "===== ModChecker 状态 =====");
                sender.sendMessage(ChatColor.YELLOW + "模式：" + ChatColor.WHITE + plugin.getMode());
                sender.sendMessage(ChatColor.YELLOW + "黑名单数量：" + ChatColor.WHITE + plugin.getBlacklistedMods().size());
                sender.sendMessage(ChatColor.YELLOW + "PacketEvents 检测：" + ChatColor.WHITE
                        + (plugin.isPacketDetectionActive() ? "已启用" : "未启用"));
                sender.sendMessage(ChatColor.YELLOW + "LuckPerms 集成：" + ChatColor.WHITE
                        + (plugin.getLuckPerms() != null ? "已启用" : "未启用"));
                return true;

            default:
                sendHelp(sender);
                return true;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== ModChecker 命令帮助 =====");
        sender.sendMessage(ChatColor.YELLOW + "/modchecker reload" + ChatColor.GRAY + " - 重载配置文件");
        sender.sendMessage(ChatColor.YELLOW + "/modchecker check <玩家>" + ChatColor.GRAY + " - 手动检查玩家");
        sender.sendMessage(ChatColor.YELLOW + "/modchecker list" + ChatColor.GRAY + " - 列出黑名单");
        sender.sendMessage(ChatColor.YELLOW + "/modchecker add <模组>" + ChatColor.GRAY + " - 添加黑名单");
        sender.sendMessage(ChatColor.YELLOW + "/modchecker remove <模组>" + ChatColor.GRAY + " - 移除黑名单");
        sender.sendMessage(ChatColor.YELLOW + "/modchecker status" + ChatColor.GRAY + " - 查看运行状态");
    }

    private boolean hasPerm(CommandSender sender, String permission) {
        if (sender.hasPermission("modchecker.command")) return true;
        return sender.hasPermission(permission);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            for (String s : Arrays.asList("reload", "check", "list", "add", "remove", "status")) {
                if (s.startsWith(args[0].toLowerCase())) subs.add(s);
            }
            return subs;
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("check")) {
                List<String> names = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        names.add(p.getName());
                    }
                }
                return names;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                List<String> mods = new ArrayList<>();
                for (String mod : plugin.getBlacklistedMods()) {
                    if (mod.startsWith(args[1].toLowerCase())) mods.add(mod);
                }
                return mods;
            }
        }
        return new ArrayList<>();
    }
}