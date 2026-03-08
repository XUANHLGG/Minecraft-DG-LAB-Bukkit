package com.tendoarisu.dglab.api;

import com.tendoarisu.dglab.core.DgLabConnection;
import com.tendoarisu.dglab.core.DgLabConnectionManager;
import com.tendoarisu.dglab.core.DgLabMessage;
import com.tendoarisu.dglab.script.wrappers.ConnectionWrapper;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ConnectionManager {
    private ConnectionManager() {}

    public static final Set<ConnectionWrapper> CONNECTIONS = new AbstractSet<>() {
        @Override
        public Iterator<ConnectionWrapper> iterator() {

            var snapshot = java.util.List.copyOf(DgLabConnectionManager.channelEntries());
            Iterator<Map.Entry<Channel, DgLabConnection>> it = snapshot.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public ConnectionWrapper next() {
                    Map.Entry<Channel, DgLabConnection> e = it.next();
                    return new ConnectionWrapper(e.getValue());
                }
            };
        }

        @Override
        public int size() {
            return DgLabConnectionManager.channelEntries().size();
        }

        @Override
        public boolean add(ConnectionWrapper connectionWrapper) {
            
            throw new UnsupportedOperationException("read-only");
        }

        @Override
        public boolean remove(Object o) {

            if (o instanceof ConnectionWrapper cw) {
                Channel ch = cw.getChannel();
                if (ch == null) return false;

                DgLabConnection removed = DgLabConnectionManager.removeByChannel(ch);
                if (removed == null) return false;

                try {
                    UUID uuid = UUID.fromString(removed.clientId());
                    DgLabConnectionManager.remove(uuid);
                } catch (Exception ignored) {
                }
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            
            DgLabConnectionManager.clearAllMappings();
        }
    };

    public static ConnectionWrapper getByPlayer(Player player) {
        if (player == null) return null;
        return getByUUID(player.getUniqueId());
    }

    public static ConnectionWrapper getByUUID(UUID uuid) {
        if (uuid == null) return null;
        DgLabConnection conn = DgLabConnectionManager.get(uuid);
        return conn == null ? null : new ConnectionWrapper(conn);
    }

    public static ConnectionWrapper getByUUID(String uuid) {
        if (uuid == null) return null;
        try {
            return getByUUID(UUID.fromString(uuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static ConnectionWrapper getByChannel(Channel channel) {
        if (channel == null) return null;
        DgLabConnection conn = DgLabConnectionManager.getByChannel(channel);
        return conn == null ? null : new ConnectionWrapper(conn);
    }

    public static void sendToAll(DgLabMessage message) {
        if (message == null) return;
        
        for (Map.Entry<Channel, DgLabConnection> e : DgLabConnectionManager.channelEntries()) {
            DgLabConnection conn = e.getValue();
            if (conn == null) continue;
            try {
                conn.sendJson(message);
            } catch (Exception ignored) {
            }
        }
    }
}
