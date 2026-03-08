package com.tendoarisu.dglab.proto;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomPayloadSender {
    private CustomPayloadSender() {}

    private static final Set<UUID> REGISTER_SENT = ConcurrentHashMap.newKeySet();

    public static void clearRegisterState(UUID playerUuid) {
        if (playerUuid == null) return;
        REGISTER_SENT.remove(playerUuid);
    }

    public static boolean send(JavaPlugin plugin, Player player, String channelId, byte[] payload) {
        if (plugin == null || player == null || channelId == null || payload == null) return false;

        if (channelId.startsWith("dglab:")) {
            UUID u = player.getUniqueId();
            if (!REGISTER_SENT.contains(u)) {
                
                String reg = String.join("\u0000",
                        DgLabChannels.STRENGTH,
                        DgLabChannels.CLEAR_STRENGTH,
                        DgLabChannels.SHOW_QR_CODE
                ) + "\u0000";
                byte[] regBytes = reg.getBytes(StandardCharsets.UTF_8);

                boolean regOk = ProtocolLibPayloadSender.send(plugin, player, "minecraft:register", regBytes);
                if (!regOk) {
                    
                    return false;
                }
                REGISTER_SENT.add(u);
            }
        }

        return ProtocolLibPayloadSender.send(plugin, player, channelId, payload);
    }
}
