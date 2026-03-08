package com.tendoarisu.dglab.core;

import io.netty.channel.Channel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DgLabConnectionManager {
    private DgLabConnectionManager() {}

    private static final Map<Channel, DgLabConnection> CONNECTIONS_BY_CHANNEL = new ConcurrentHashMap<>();
    private static final Map<UUID, DgLabConnection> CONNECTIONS_BY_UUID = new ConcurrentHashMap<>();

    public static void clearAllMappings() {
        CONNECTIONS_BY_CHANNEL.clear();
        CONNECTIONS_BY_UUID.clear();
    }

    public static void putByChannel(Channel channel, DgLabConnection connection) {
        if (channel == null || connection == null) return;
        CONNECTIONS_BY_CHANNEL.put(channel, connection);
    }

    public static DgLabConnection getByChannel(Channel channel) {
        if (channel == null) return null;
        return CONNECTIONS_BY_CHANNEL.get(channel);
    }

    public static DgLabConnection removeByChannel(Channel channel) {
        if (channel == null) return null;
        return CONNECTIONS_BY_CHANNEL.remove(channel);
    }

    public static Set<Map.Entry<Channel, DgLabConnection>> channelEntries() {
        return CONNECTIONS_BY_CHANNEL.entrySet();
    }

    public static void put(UUID playerUuid, DgLabConnection connection) {
        if (playerUuid == null || connection == null) return;
        CONNECTIONS_BY_UUID.put(playerUuid, connection);
        
        putByChannel(connection.channel(), connection);
    }

    public static DgLabConnection get(UUID playerUuid) {
        if (playerUuid == null) return null;
        return CONNECTIONS_BY_UUID.get(playerUuid);
    }

    public static void remove(UUID playerUuid) {
        if (playerUuid == null) return;
        CONNECTIONS_BY_UUID.remove(playerUuid);
    }

    public static Set<Map.Entry<UUID, DgLabConnection>> entries() {
        return CONNECTIONS_BY_UUID.entrySet();
    }

    public static List<UUID> getOnlineUUIDs() {
        return List.copyOf(CONNECTIONS_BY_UUID.keySet());
    }

    public static void disconnectByPlayer(UUID playerUuid) {
        if (playerUuid == null) return;
        DgLabConnection conn = CONNECTIONS_BY_UUID.remove(playerUuid);
        if (conn != null) {
            
            try {
                removeByChannel(conn.channel());
            } catch (Exception ignored) {
            }
            conn.disconnect();
        }
    }

    public static void disconnectAll() {
        
        for (DgLabConnection conn : List.copyOf(CONNECTIONS_BY_CHANNEL.values())) {
            if (conn == null) continue;
            try {
                conn.disconnect();
            } catch (Exception ignored) {
            }
        }
        clearAllMappings();
    }
}
