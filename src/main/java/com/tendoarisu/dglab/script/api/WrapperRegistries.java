package com.tendoarisu.dglab.script.api;

import com.tendoarisu.dglab.api.ChannelType;
import com.tendoarisu.dglab.api.ConnectionManager;
import com.tendoarisu.dglab.core.DgLabMessage;
import com.tendoarisu.dglab.core.DgLabMessageType;
import com.tendoarisu.dglab.script.util.DgLabPulseUtil;

import org.bukkit.plugin.java.JavaPlugin;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.Scriptable;

public final class WrapperRegistries {
    private WrapperRegistries() {}

    public static void bindAll(JavaPlugin plugin, Scriptable scope) {

        Context cx = Context.getCurrentContext();
        if (cx == null) {
            return;
        }

        scope.put("ChannelType", scope, new NativeJavaClass(scope, ChannelType.class));
        scope.put("DgLabMessage", scope, new NativeJavaClass(scope, DgLabMessage.class));
        scope.put("DgLabMessageType", scope, new NativeJavaClass(scope, DgLabMessageType.class));
        
        scope.put("DgLabManager", scope, new NativeJavaClass(scope, ConnectionManager.class));
        scope.put("DgLabPulseUtil", scope, new NativeJavaClass(scope, DgLabPulseUtil.class));
    }
}
