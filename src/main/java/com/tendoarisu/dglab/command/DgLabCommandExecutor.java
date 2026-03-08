package com.tendoarisu.dglab.command;

import com.tendoarisu.dglab.bukkit;
import com.tendoarisu.dglab.core.DgLabConnectionManager;
import com.tendoarisu.dglab.proto.DgLabChannels;
import com.tendoarisu.dglab.proto.ShowQrCodePayload;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class DgLabCommandExecutor implements CommandExecutor, TabCompleter {

    private final bukkit plugin;

    public DgLabCommandExecutor(bukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "/dglab connect" + ChatColor.GRAY + " - 显示二维码 (需要客户端模组)");
            sender.sendMessage(ChatColor.YELLOW + "/dglab disconnect" + ChatColor.GRAY + " - 断开 DG-LAB 连接");
            sender.sendMessage(ChatColor.YELLOW + "/dglab reload" + ChatColor.GRAY + " - 重载配置与脚本");
            sender.sendMessage(ChatColor.GRAY + "脚本文件夹: plugins/" + plugin.getName() + "/scripts");
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "connect" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "只有玩家才能使用此命令。");
                    return true;
                }

                if (!plugin.getConfig().getBoolean("websocket.enabled", false) || !plugin.isWebSocketRunning()) {
                    sender.sendMessage(ChatColor.RED + "WebSocket 服务器未运行。请在 config.yml 中启用并重载插件。");
                    return true;
                }

                String qr = plugin.buildQrUrl(player.getUniqueId());

                com.tendoarisu.dglab.proto.CustomPayloadSender.send(
                        plugin,
                        player,
                        DgLabChannels.SHOW_QR_CODE,
                        new ShowQrCodePayload(qr).encode()
                );

                sender.sendMessage(ChatColor.GREEN + "二维码已发送至客户端（如果你已安装模组）。");
                return true;
            }
            case "disconnect" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(ChatColor.RED + "只有玩家才能使用此命令。");
                    return true;
                }

                com.tendoarisu.dglab.proto.CustomPayloadSender.send(
                        plugin,
                        player,
                        DgLabChannels.SHOW_QR_CODE,
                        new ShowQrCodePayload("").encode()
                );

                var conn = DgLabConnectionManager.get(player.getUniqueId());
                if (conn != null) {
                    DgLabConnectionManager.disconnectByPlayer(player.getUniqueId());
                    sender.sendMessage(ChatColor.GREEN + "已断开连接。");
                } else {
                    sender.sendMessage(ChatColor.RED + "未连接。");
                }
                return true;
            }
            case "reload" -> {
                if (!sender.hasPermission("dglab.admin")) {
                    sender.sendMessage(ChatColor.RED + "没有权限。");
                    return true;
                }
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "配置与脚本重载完成。");
                return true;
            }
            default -> {
                sender.sendMessage(ChatColor.RED + "未知的子命令。");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("connect", "disconnect"));
            if (sender.hasPermission("dglab.admin")) subs.add("reload");
            return subs;
        }
        return List.of();
    }
}
