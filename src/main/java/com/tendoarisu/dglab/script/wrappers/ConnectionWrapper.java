package com.tendoarisu.dglab.script.wrappers;

import com.tendoarisu.dglab.api.ChannelType;
import com.tendoarisu.dglab.core.DgLabConnection;
import com.tendoarisu.dglab.core.DgLabMessage;
import com.tendoarisu.dglab.script.util.DgLabPulseUtil;

import io.netty.channel.Channel;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ConnectionWrapper {

    private final DgLabConnection conn;

    public ConnectionWrapper(DgLabConnection conn) {
        this.conn = Objects.requireNonNull(conn, "conn");
    }

    public Channel getChannel() {
        return conn.channel();
    }

    public StrengthWrapper getStrength() {
        return new StrengthWrapper(conn);
    }

    public String getClientId() {
        return conn.clientId();
    }

    public String getTargetId() {
        return conn.targetId();
    }

    public void sendMessage(DgLabMessage message) {
        if (message == null) return;
        conn.sendJson(message);
    }

    public void sendMessage(String message) {
        if (message == null) return;
        conn.sendText(message);
    }

    public void sendMessage(Object message) {
        if (message == null) return;

        if (message instanceof NativeJavaObject njo) {
            Object u = njo.unwrap();
            if (u != null) {
                sendMessage(u);
                return;
            }
        }

        if (message instanceof DgLabMessage m) {
            conn.sendJson(m);
            return;
        }
        if (message instanceof String s) {
            conn.sendText(s);
            return;
        }
        conn.sendText(String.valueOf(message));
    }

    private static ChannelType asChannelType(Object channel) {
        if (channel == null) return ChannelType.A;
        if (channel instanceof ChannelType ct) return ct;
        if (channel instanceof NativeJavaObject njo) {
            Object u = njo.unwrap();
            if (u instanceof ChannelType ct) return ct;
            channel = u;
        }
        if (channel instanceof String s) {
            return "b".equalsIgnoreCase(s) ? ChannelType.B : ChannelType.A;
        }
        
        return ChannelType.A;
    }

    private static int typeNumberFrom(Object channel) {
        return asChannelType(channel).getTypeNumber();
    }

    private static String channelNameFrom(Object channel) {
        return asChannelType(channel).name(); 
    }

    public void addStrength(Object channel, int value) {
        conn.addStrength(typeNumberFrom(channel), value);
    }

    public void addStrength(ChannelType channel, int value) {
        addStrength((Object) channel, value);
    }

    public void reduceStrength(Object channel, int value) {
        conn.reduceStrength(typeNumberFrom(channel), value);
    }

    public void reduceStrength(ChannelType channel, int value) {
        reduceStrength((Object) channel, value);
    }

    public void setStrength(Object channel, int value) {
        conn.setStrength(typeNumberFrom(channel), value);
    }

    public void setStrength(ChannelType channel, int value) {
        setStrength((Object) channel, value);
    }

    public void clearPulse(Object channel) {
        conn.clearPulse(typeNumberFrom(channel));
    }

    public void clearPulse(ChannelType channel) {
        clearPulse((Object) channel);
    }

    public void clear(Object channel) {
        clearPulse(channel);
    }

    public void addPulse(Object channel, Object pulse) {
        String chanName = channelNameFrom(channel);
        if (pulse == null) {
            return;
        }

        if (pulse instanceof NativeJavaObject njo) {
            Object u = njo.unwrap();
            if (u != null) {
                addPulse(channel, u);
                return;
            }
        }

        if (pulse instanceof String s) {
            conn.addPulse(chanName, s);
            return;
        }

        if (pulse instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object o : list) {
                if (o == null) continue;
                if (o instanceof NativeJavaObject njo) {
                    Object unwrapped = njo.unwrap();
                    if (unwrapped != null) {
                        out.add(String.valueOf(unwrapped));
                        continue;
                    }
                }
                out.add(String.valueOf(o));
            }
            conn.addPulse(chanName, DgLabPulseUtil.toStringArray(out));
            return;
        }

        if (pulse instanceof NativeArray arr) {
            List<String> out = new ArrayList<>();
            for (Object o : arr) {
                if (o == null) continue;
                if (o instanceof NativeJavaObject njo) {
                    Object unwrapped = njo.unwrap();
                    if (unwrapped != null) {
                        out.add(String.valueOf(unwrapped));
                        continue;
                    }
                }
                out.add(String.valueOf(o));
            }
            conn.addPulse(chanName, DgLabPulseUtil.toStringArray(out));
            return;
        }

        conn.addPulse(chanName, String.valueOf(pulse));
    }

    public void addPulse(ChannelType channel, List<String> pulse) {
        addPulse((Object) channel, pulse);
    }

    public void disconnect() {
        conn.disconnect();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConnectionWrapper that)) return false;
        return this.conn.channel() == that.conn.channel();
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(conn.channel());
    }

    @Override
    public String toString() {
        return "ConnectionWrapper{clientId=" + conn.clientId() + ", targetId=" + conn.targetId() + "}";
    }
}
