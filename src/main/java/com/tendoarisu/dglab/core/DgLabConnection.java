package com.tendoarisu.dglab.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tendoarisu.dglab.proto.ClearStrengthPayload;
import com.tendoarisu.dglab.proto.DgLabChannels;
import com.tendoarisu.dglab.proto.ShowQrCodePayload;
import com.tendoarisu.dglab.proto.StrengthPayload;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DgLabConnection {

    private static final Gson GSON = new GsonBuilder().create();

    private static final Pattern STRENGTH_PATTERN =
            Pattern.compile("strength-(\\d+)\\+(\\d+)\\+(\\d+)\\+(\\d+)", Pattern.MULTILINE);

    private final JavaPlugin plugin;
    private final Channel channel;

    private volatile String clientId = ""; 
    private volatile String targetId = "";

    private volatile int aCurrent = 0;
    private volatile int bCurrent = 0;
    private volatile int aMax = 0;
    private volatile int bMax = 0;

    public DgLabConnection(JavaPlugin plugin, Channel channel) {
        this.plugin = Objects.requireNonNull(plugin);
        this.channel = Objects.requireNonNull(channel);
    }

    public Channel channel() {
        return channel;
    }

    public String clientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId == null ? "" : clientId;
    }

    public String targetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId == null ? "" : targetId;
    }

    public void handleTextFrame(String text) {
        DgLabMessage message;
        try {
            message = GSON.fromJson(text, DgLabMessage.class);
            Objects.requireNonNull(message.type());
            Objects.requireNonNull(message.clientId());
            Objects.requireNonNull(message.targetId());
            Objects.requireNonNull(message.message());
        } catch (Exception ex) {
            
            sendJson(DgLabMessage.msg("", "", "403"));
            return;
        }

        switch (message.type()) {
            case HEARTBEAT -> {
                
            }
            case BIND -> sendJson(DgLabMessage.bind(message.clientId(), message.targetId(), "200"));
            case MSG -> {
                Matcher matcher = STRENGTH_PATTERN.matcher(message.message());
                if (matcher.find()) {
                    aCurrent = Integer.parseInt(matcher.group(1));
                    bCurrent = Integer.parseInt(matcher.group(2));
                    aMax = Integer.parseInt(matcher.group(3));
                    bMax = Integer.parseInt(matcher.group(4));

                    runSync(() -> {
                        Player player = getPlayer();
                        if (player != null) {
                            sendPluginMessage(
                                    player,
                                    DgLabChannels.STRENGTH,
                                    new StrengthPayload(aCurrent, bCurrent, aMax, bMax).encode());
                        }
                    });
                }
            }
            default -> {
                
            }
        }
    }

    public void onHandshakeComplete(String clientIdFromUri) {
        setClientId(clientIdFromUri);

        String newTarget = UUID.randomUUID().toString();
        setTargetId(newTarget);
        sendJson(DgLabMessage.bind(newTarget, "", "targetId"));

        runSync(() -> {
            Player player = getPlayer();
            if (player != null) {
                sendPluginMessage(player, DgLabChannels.SHOW_QR_CODE, new ShowQrCodePayload("").encode());
            }
        });
    }

    public void sendJson(DgLabMessage message) {
        if (message == null) return;
        sendText(message.toJson());
    }

    public void sendText(String json) {
        channel.writeAndFlush(new TextWebSocketFrame(json));
    }

    public void disconnect() {
        
        sendJson(new DgLabMessage(DgLabMessageType.BREAK, clientId, targetId, "209"));
        channel.close();
    }

    public void reduceStrength(int typeNumber, int value) {
        sendJson(DgLabMessage.msg(clientId, targetId, "strength-%d+0+%d".formatted(typeNumber, value)));
    }

    public void addStrength(int typeNumber, int value) {
        sendJson(DgLabMessage.msg(clientId, targetId, "strength-%d+1+%d".formatted(typeNumber, value)));
    }

    public void setStrength(int typeNumber, int value) {
        sendJson(DgLabMessage.msg(clientId, targetId, "strength-%d+2+%d".formatted(typeNumber, value)));
    }

    public void addPulse(String channelName, String pulseStringArray) {
        
        sendJson(DgLabMessage.msg(clientId, targetId, "pulse-%s:%s".formatted(channelName, pulseStringArray)));
    }

    public void clearPulse(int typeNumber) {
        sendJson(DgLabMessage.msg(clientId, targetId, "clear-%s".formatted(typeNumber)));
    }

    public void onChannelInactive() {
        runSync(() -> {
            Player player = getPlayer();
            if (player != null) {
                sendPluginMessage(player, DgLabChannels.CLEAR_STRENGTH, ClearStrengthPayload.EMPTY);
                sendPluginMessage(player, DgLabChannels.SHOW_QR_CODE, new ShowQrCodePayload("").encode());
            }
        });
    }

    private Player getPlayer() {
        if (clientId == null || clientId.isEmpty()) return null;
        try {
            UUID uuid = UUID.fromString(clientId);
            return Bukkit.getPlayer(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void sendPluginMessage(Player player, String channel, byte[] payload) {

        com.tendoarisu.dglab.proto.CustomPayloadSender.send(plugin, player, channel, payload);
    }

    private void runSync(Runnable r) {
        if (Bukkit.isPrimaryThread()) {
            r.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, r);
        }
    }

    public int getACurrent() { return aCurrent; }
    public int getBCurrent() { return bCurrent; }
    public int getAMax() { return aMax; }
    public int getBMax() { return bMax; }
}
