package com.tendoarisu.dglab.proto;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.MinecraftKey;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ProtocolLibPayloadSender {
    private ProtocolLibPayloadSender() {}

    public static boolean isAvailable() {
        try {
            Class.forName("com.comphenix.protocol.ProtocolLibrary");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean send(Plugin plugin, Player player, String channelId, byte[] payload) {
        if (plugin == null || player == null || channelId == null || payload == null) return false;
        if (!isAvailable()) return false;

        PacketType type = resolveCustomPayloadType();
        if (type == null) return false;

        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = new PacketContainer(type);

            if (tryWriteViaAccessors(packet, channelId, payload)) {
                pm.sendServerPacket(player, packet);
                return true;
            }

            if (tryWriteViaFieldScan(packet, channelId, payload)) {
                pm.sendServerPacket(player, packet);
                return true;
            }

            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryWriteViaAccessors(PacketContainer packet, String channelId, byte[] payload) {
        boolean wroteChannel = false;
        try {
            
            packet.getMinecraftKeys().write(0, new MinecraftKey(channelId));
            wroteChannel = true;
        } catch (Throwable ignored) {
        }
        if (!wroteChannel) {
            try {
                
                packet.getStrings().write(0, channelId);
                wroteChannel = true;
            } catch (Throwable ignored) {
            }
        }

        boolean wrotePayload = false;
        try {
            
            packet.getByteArrays().write(0, payload);
            wrotePayload = true;
        } catch (Throwable ignored) {
        }

        return wroteChannel && wrotePayload;
    }

    private static boolean tryWriteViaFieldScan(PacketContainer packet, String channelId, byte[] payload) {
        StructureModifier<Object> mod;
        try {
            mod = packet.getModifier();
        } catch (Throwable ignored) {
            return false;
        }

        List<?> accessors;
        try {
            accessors = mod.getFields();
        } catch (Throwable ignored) {
            return false;
        }

        Integer idxChannel = null;
        Integer idxData = null;
        Integer idxPayloadObj = null;

        for (int i = 0; i < accessors.size(); i++) {
            Class<?> t = null;
            try {
                t = modifierFieldType(mod, i);
            } catch (Throwable ignored) {
            }
            if (t == null) {
                try {
                    t = fieldAccessorType(accessors.get(i));
                } catch (Throwable ignored) {
                }
            }
            if (t == null) continue;

            if (idxPayloadObj == null && t.getName().equals("net.minecraft.network.protocol.common.custom.CustomPacketPayload")) {
                idxPayloadObj = i;
                continue;
            }

            if (idxChannel == null) {
                if (t == String.class) idxChannel = i;
                else if (t == MinecraftKey.class) idxChannel = i;
                else if (t.getName().equals("net.minecraft.resources.ResourceLocation")) idxChannel = i;
            }

            if (idxData == null) {
                if (t == byte[].class) idxData = i;
                else if (t.getName().equals("io.netty.buffer.ByteBuf")) idxData = i;
                else if (t.getName().equals("net.minecraft.network.FriendlyByteBuf")) idxData = i;
            }
        }

        if (idxPayloadObj != null || accessors.size() == 1) {
            Object payloadObj = buildCustomPacketPayload(channelId, payload);
            if (payloadObj == null) return false;

            int start = (idxPayloadObj != null) ? idxPayloadObj : 0;
            try {
                mod.write(start, payloadObj);
                return true;
            } catch (Throwable ignored) {
                
                for (int i = 0; i < accessors.size(); i++) {
                    try {
                        mod.write(i, payloadObj);
                        return true;
                    } catch (Throwable ignored2) {
                    }
                }
                return false;
            }
        }

        if (idxChannel == null || idxData == null) return false;

        try {
            Class<?> channelType = modifierFieldType(mod, idxChannel);
            if (channelType == null) channelType = fieldAccessorType(accessors.get(idxChannel));

            Object channelValue;
            if (channelType == String.class) channelValue = channelId;
            else if (channelType == MinecraftKey.class) channelValue = new MinecraftKey(channelId);
            else if (channelType != null && channelType.getName().equals("net.minecraft.resources.ResourceLocation")) {
                channelValue = newResourceLocation(channelId);
            } else {
                return false;
            }
            mod.write(idxChannel, channelValue);

            Class<?> dataType = modifierFieldType(mod, idxData);
            if (dataType == null) dataType = fieldAccessorType(accessors.get(idxData));

            Object dataValue;
            if (dataType == byte[].class) dataValue = payload;
            else if (dataType != null && dataType.getName().equals("io.netty.buffer.ByteBuf")) dataValue = Unpooled.wrappedBuffer(payload);
            else if (dataType != null && dataType.getName().equals("net.minecraft.network.FriendlyByteBuf")) dataValue = newFriendlyByteBuf(payload);
            else {
                return false;
            }
            mod.write(idxData, dataValue);

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object buildCustomPacketPayload(String channelId, byte[] payload) {
        
        if ("minecraft:register".equals(channelId) || "minecraft:unregister".equals(channelId)) {
            boolean unregister = "minecraft:unregister".equals(channelId);
            Object v = tryConstructMinecraftRegisterPayload(unregister, payload);
            if (v != null) return v;
        }

        for (String className : new String[] {
                "net.minecraft.network.protocol.common.custom.DiscardedPayload",
                "net.minecraft.network.protocol.common.custom.UnknownPayload"
        }) {
            Object v = tryConstructUnknownPayload(className, channelId, payload);
            if (v != null) return v;
        }

        return null;
    }

    private static Object tryConstructMinecraftRegisterPayload(boolean unregister, byte[] payload) {
        List<String> channels = parseNullSeparatedChannelList(payload);
        if (channels.isEmpty()) return null;

        String[] candidates = unregister
                ? new String[] {
                
                "net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload",
                "net.neoforged.neoforge.network.payload.MinecraftUnRegisterPayload",

                "net.minecraft.network.protocol.common.custom.MinecraftUnregisterPayload",
                "net.minecraft.network.protocol.common.custom.UnregisterPayload",

                "net.minecraft.network.protocol.common.custom.CustomPacketPayloads$MinecraftUnregisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPacketPayloads$UnregisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPacketPayloads$Unregister",
                "net.minecraft.network.protocol.common.custom.CustomPayloads$MinecraftUnregisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPayloads$UnregisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPayloads$Unregister"
        }
                : new String[] {
                
                "net.neoforged.neoforge.network.payload.MinecraftRegisterPayload",

                "net.minecraft.network.protocol.common.custom.MinecraftRegisterPayload",
                "net.minecraft.network.protocol.common.custom.RegisterPayload",

                "net.minecraft.network.protocol.common.custom.CustomPacketPayloads$MinecraftRegisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPacketPayloads$RegisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPacketPayloads$Register",
                "net.minecraft.network.protocol.common.custom.CustomPayloads$MinecraftRegisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPayloads$RegisterPayload",
                "net.minecraft.network.protocol.common.custom.CustomPayloads$Register"
        };

        for (String className : candidates) {
            Object v = tryConstructRegisterLikePayload(className, channels);
            if (v != null) return v;
        }

        return null;
    }

    private static List<String> parseNullSeparatedChannelList(byte[] payload) {
        if (payload == null || payload.length == 0) return List.of();

        final String s;
        try {
            s = new String(payload, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
            return List.of();
        }

        String[] parts = s.split("\u0000", -1);
        List<String> out = new ArrayList<>();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            out.add(p);
        }
        return out;
    }

    private static Object tryConstructRegisterLikePayload(String className, List<String> channels) {
        final Class<?> c;
        try {
            c = Class.forName(className);
        } catch (Throwable ignored) {
            return null;
        }

        try {
            
            for (Constructor<?> ctor : c.getDeclaredConstructors()) {
                if (ctor.getParameterCount() != 1) continue;
                Class<?> p0 = ctor.getParameterTypes()[0];

                Object arg = null;

                if (p0.isArray()) {
                    Class<?> elemType = p0.getComponentType();
                    Object arr = java.lang.reflect.Array.newInstance(elemType, channels.size());
                    boolean ok = true;
                    for (int i = 0; i < channels.size(); i++) {
                        Object id = buildIdValue(elemType, channels.get(i));
                        if (id == null) {
                            ok = false;
                            break;
                        }
                        java.lang.reflect.Array.set(arr, i, id);
                    }
                    if (ok) arg = arr;
                }

                if (arg == null && (p0 == List.class || p0 == Collection.class || Iterable.class.isAssignableFrom(p0) || Collection.class.isAssignableFrom(p0))) {
                    Class<?> elemType = inferCollectionElementType(c);

                    final Collection<Object> coll;
                    if (List.class.isAssignableFrom(p0)) coll = new ArrayList<>();
                    else if (Set.class.isAssignableFrom(p0)) coll = new HashSet<>();
                    else coll = new ArrayList<>();

                    boolean ok = true;
                    for (String ch : channels) {
                        Object id;
                        if (elemType == null || elemType == String.class) id = ch;
                        else id = buildIdValue(elemType, ch);
                        if (id == null) {
                            ok = false;
                            break;
                        }
                        coll.add(id);
                    }
                    if (ok) arg = coll;
                }

                if (arg == null && p0 == String.class) {
                    arg = String.join("\u0000", channels);
                }

                if (arg == null) continue;

                try {
                    ctor.setAccessible(true);
                    return ctor.newInstance(arg);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Class<?> inferCollectionElementType(Class<?> payloadClass) {
        if (payloadClass == null) return null;

        try {
            if (payloadClass.isRecord()) {
                for (RecordComponent rc : payloadClass.getRecordComponents()) {
                    Class<?> t = extractFirstTypeArg(rc.getGenericType());
                    if (t != null) return t;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Field f : payloadClass.getDeclaredFields()) {
                Class<?> t = extractFirstTypeArg(f.getGenericType());
                if (t != null) return t;
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Method m : payloadClass.getDeclaredMethods()) {
                Class<?> t = extractFirstTypeArg(m.getGenericReturnType());
                if (t != null) return t;
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static Class<?> extractFirstTypeArg(Type genericType) {
        if (!(genericType instanceof ParameterizedType pt)) return null;
        Type[] args = pt.getActualTypeArguments();
        if (args.length < 1) return null;

        Type a0 = args[0];
        if (a0 instanceof Class<?> c) return c;

        if (a0 instanceof ParameterizedType pt2 && pt2.getRawType() instanceof Class<?> raw) return raw;

        return null;
    }

    private static Object tryConstructUnknownPayload(String className, String channelId, byte[] payload) {
        try {
            Class<?> c = Class.forName(className);
            for (Constructor<?> ctor : c.getDeclaredConstructors()) {
                try {
                    if (ctor.getParameterCount() != 2) continue;
                    Class<?> idType = ctor.getParameterTypes()[0];
                    Class<?> dataType = ctor.getParameterTypes()[1];

                    Object id = buildIdValue(idType, channelId);
                    if (id == null) continue;
                    Object data = buildDataValue(dataType, payload);
                    if (data == null) continue;

                    if (!idType.isInstance(id) && !idType.isAssignableFrom(id.getClass())) continue;
                    if (!dataType.isInstance(data) && !dataType.isAssignableFrom(data.getClass())) continue;

                    ctor.setAccessible(true);
                    return ctor.newInstance(id, data);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object buildIdValue(Class<?> idType, String channelId) {
        if (idType == String.class) return channelId;

        for (String m : new String[] {"parse", "tryParse", "of", "fromString"}) {
            try {
                Method mm = idType.getDeclaredMethod(m, String.class);
                mm.setAccessible(true);
                Object v = mm.invoke(null, channelId);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }

        try {
            Constructor<?> ctor = idType.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(channelId);
        } catch (Throwable ignored) {
        }

        int colon = channelId.indexOf(':');
        if (colon > 0 && colon < channelId.length() - 1) {
            String ns = channelId.substring(0, colon);
            String path = channelId.substring(colon + 1);
            try {
                Constructor<?> ctor = idType.getDeclaredConstructor(String.class, String.class);
                ctor.setAccessible(true);
                return ctor.newInstance(ns, path);
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Object buildDataValue(Class<?> dataType, byte[] payload) {
        if (dataType == byte[].class) return payload;

        if (dataType.getName().equals("io.netty.buffer.ByteBuf") || dataType.isAssignableFrom(Unpooled.wrappedBuffer(payload).getClass())) {
            return Unpooled.wrappedBuffer(payload);
        }

        Object nettyBuf = Unpooled.wrappedBuffer(payload);
        for (Constructor<?> ctor : dataType.getDeclaredConstructors()) {
            try {
                if (ctor.getParameterCount() != 1) continue;
                Class<?> p0 = ctor.getParameterTypes()[0];
                if (!(p0.isInstance(nettyBuf) || p0.isAssignableFrom(nettyBuf.getClass()))) continue;
                ctor.setAccessible(true);
                return ctor.newInstance(nettyBuf);
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Object newResourceLocation(String id) throws Exception {
        Class<?> rl = Class.forName("net.minecraft.resources.ResourceLocation");

        for (String m : new String[] {"parse", "tryParse", "of"}) {
            try {
                Method mm = rl.getDeclaredMethod(m, String.class);
                mm.setAccessible(true);
                Object v = mm.invoke(null, id);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }

        try {
            Constructor<?> ctor = rl.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(id);
        } catch (NoSuchMethodException ignored) {
        }

        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) throw new IllegalArgumentException("Invalid namespaced id: " + id);
        String ns = id.substring(0, colon);
        String path = id.substring(colon + 1);
        Constructor<?> ctor = rl.getDeclaredConstructor(String.class, String.class);
        ctor.setAccessible(true);
        return ctor.newInstance(ns, path);
    }

    private static Object newFriendlyByteBuf(byte[] payload) throws Exception {
        Object nettyBuf = Unpooled.wrappedBuffer(payload);
        Class<?> fbb = Class.forName("net.minecraft.network.FriendlyByteBuf");
        for (Constructor<?> ctor : fbb.getDeclaredConstructors()) {
            try {
                if (ctor.getParameterCount() != 1) continue;
                Class<?> p0 = ctor.getParameterTypes()[0];
                if (!(p0.isInstance(nettyBuf) || p0.isAssignableFrom(nettyBuf.getClass()))) continue;
                ctor.setAccessible(true);
                return ctor.newInstance(nettyBuf);
            } catch (Throwable ignored) {
            }
        }
        throw new NoSuchMethodException("No FriendlyByteBuf(ByteBuf) constructor found");
    }

    private static Class<?> fieldAccessorType(Object accessor) {
        if (accessor == null) return null;
        if (accessor instanceof Field f) return f.getType();

        try {
            Method m = accessor.getClass().getMethod("getFieldType");
            Object v = m.invoke(accessor);
            if (v instanceof Class<?> c) return c;
        } catch (Throwable ignored) {
        }

        try {
            Method m = accessor.getClass().getMethod("getField");
            Object v = m.invoke(accessor);
            if (v instanceof Field f) return f.getType();
        } catch (Throwable ignored) {
        }

        for (String name : new String[] {"getType", "type"}) {
            try {
                Method m = accessor.getClass().getMethod(name);
                Object v = m.invoke(accessor);
                if (v instanceof Class<?> c) return c;
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static Class<?> modifierFieldType(StructureModifier<?> mod, int index) {
        if (mod == null) return null;

        try {
            Method m = mod.getClass().getMethod("getFieldType", int.class);
            Object v = m.invoke(mod, index);
            if (v instanceof Class<?> c) return c;
        } catch (Throwable ignored) {
        }

        try {
            Method m = mod.getClass().getMethod("getField", int.class);
            Object v = m.invoke(mod, index);
            if (v instanceof Field f) return f.getType();
            if (v instanceof Class<?> c) return c;
        } catch (Throwable ignored) {
        }

        for (String name : new String[] {"fieldType", "type", "getType"}) {
            try {
                Method m = mod.getClass().getMethod(name, int.class);
                Object v = m.invoke(mod, index);
                if (v instanceof Class<?> c) return c;
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static PacketType resolveCustomPayloadType() {
        for (String name : new String[] {"CUSTOM_PAYLOAD", "PLUGIN_MESSAGE"}) {
            try {
                Field f = PacketType.Play.Server.class.getField(name);
                Object v = f.get(null);
                if (v instanceof PacketType pt) return pt;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
