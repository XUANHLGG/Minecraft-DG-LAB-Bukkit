package com.tendoarisu.dglab.proto;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public record ShowQrCodePayload(String text) {

    public byte[] encode() {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(VarInt.writeVarInt(bytes.length));
        out.writeBytes(bytes);
        return out.toByteArray();
    }
}
