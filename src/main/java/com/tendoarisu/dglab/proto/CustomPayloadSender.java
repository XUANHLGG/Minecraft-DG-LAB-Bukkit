package com.tendoarisu.dglab.proto;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomPayloadSender {
    private CustomPayloadSender() {}

    public static boolean send(JavaPlugin plugin, Player player, String channelId, byte[] payload) {
        if (plugin == null || player == null || channelId == null || payload == null) return false;

        try {
            player.sendPluginMessage(plugin, channelId, payload);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("发送自定义数据包失败: " + channelId + " - " + e.getMessage());
            return false;
        }
    }
}
