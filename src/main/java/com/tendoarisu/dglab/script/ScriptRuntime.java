package com.tendoarisu.dglab.script;

import com.tendoarisu.dglab.script.api.EntityEvents;
import com.tendoarisu.dglab.script.api.WrapperRegistries;
import com.tendoarisu.dglab.script.events.EventBus;
import com.tendoarisu.dglab.script.events.EventType;

import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public final class ScriptRuntime {

    private final JavaPlugin plugin;

    private volatile Scriptable globalScope;

    public ScriptRuntime(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void resetScope() {
        Context cx = SandboxContextFactory.instance().enter();
        try {
            
            Scriptable scope = cx.initStandardObjects(null, false);

            WrapperRegistries.bindAll(plugin, scope);
            scope.put("EntityEvents", scope, new EntityEvents());

            scope.put("console", scope, new com.tendoarisu.dglab.script.api.ScriptLogger(plugin));

            this.globalScope = scope;

            EventBus.clearAll();
        } finally {
            SandboxContextFactory.instance().exit();
        }
    }

    public Scriptable scope() {
        return globalScope;
    }

    public void fire(EventType type, Object eventObj) {
        Scriptable scope = globalScope;
        if (scope == null) return;

        Context cx = SandboxContextFactory.instance().enter();
        try {
            EventBus.fire(type, scope, eventObj, t -> {
                plugin.getLogger().severe("[脚本] 执行事件回调 (" + type + ") 时抛出异常: " + t.getMessage());
            });
        } finally {
            SandboxContextFactory.instance().exit();
        }
    }
}
