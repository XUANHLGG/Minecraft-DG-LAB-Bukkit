package com.tendoarisu.dglab.script.events;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class EventBus {
    private EventBus() {}

    private static final Map<EventType, List<Function>> LISTENERS = new ConcurrentHashMap<>();

    public static void clearAll() {
        LISTENERS.clear();
    }

    public static void register(EventType type, Function callback) {
        if (type == null || callback == null) return;
        LISTENERS.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public static void fire(EventType type, Scriptable scope, Object eventObj, Consumer<Throwable> onError) {
        List<Function> list = LISTENERS.get(type);
        if (list == null || list.isEmpty()) return;

        Context cx = Context.getCurrentContext();
        if (cx == null) {
            
            return;
        }

        for (Function fn : list) {
            try {
                fn.call(cx, scope, scope, new Object[]{eventObj});
            } catch (Throwable t) {
                if (onError != null) {
                    try {
                        onError.accept(t);
                    } catch (Throwable ignored) {
                        
                    }
                }
            }
        }
    }
}
