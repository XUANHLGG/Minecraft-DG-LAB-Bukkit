package com.tendoarisu.dglab.proto;

import java.io.ByteArrayOutputStream;

public record StrengthPayload(int aCurrent, int bCurrent, int aMax, int bMax) {

    public byte[] encode() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, aCurrent);
        write(out, bCurrent);
        write(out, aMax);
        write(out, bMax);
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, int value) {
        byte[] v = VarInt.writeVarInt(value);
        out.writeBytes(v);
    }
}
