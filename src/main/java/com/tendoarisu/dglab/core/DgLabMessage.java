package com.tendoarisu.dglab.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public record DgLabMessage(DgLabMessageType type, String clientId, String targetId, String message) {

    private static final Gson GSON = new GsonBuilder().create();

    public String toJson() {
        return GSON.toJson(this);
    }

    public static DgLabMessage msg(String clientId, String targetId, String message) {
        return new DgLabMessage(DgLabMessageType.MSG, clientId, targetId, message);
    }

    public static DgLabMessage bind(String clientId, String targetId, String message) {
        return new DgLabMessage(DgLabMessageType.BIND, clientId, targetId, message);
    }
}
