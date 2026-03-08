package com.tendoarisu.dglab.proto;

import java.io.ByteArrayOutputStream;

public final class VarInt {
    private VarInt() {}

    public static byte[] writeVarInt(int value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(5);
        int v = value;
        while ((v & 0xFFFFFF80) != 0) {
            out.write((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        out.write(v & 0x7F);
        return out.toByteArray();
    }
}
