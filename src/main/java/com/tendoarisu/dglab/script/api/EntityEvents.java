package com.tendoarisu.dglab.script.api;

import com.tendoarisu.dglab.script.events.EventBus;
import com.tendoarisu.dglab.script.events.EventType;

import org.mozilla.javascript.Function;

public final class EntityEvents {

    public void death(String entityType, Function callback) {
        if (callback == null) return;
        if (!"player".equalsIgnoreCase(entityType)) {
            return;
        }
        EventBus.register(EventType.PLAYER_DEATH, callback);
    }

    public void afterHurt(String entityType, Function callback) {
        if (callback == null) return;
        if (!"player".equalsIgnoreCase(entityType)) {
            return;
        }
        EventBus.register(EventType.PLAYER_AFTER_HURT, callback);
    }

    public void playerDeath(Function callback) {
        death("player", callback);
    }

    public void playerAfterHurt(Function callback) {
        afterHurt("player", callback);
    }
}
