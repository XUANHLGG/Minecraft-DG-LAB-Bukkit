package com.tendoarisu.dglab.script;

import org.mozilla.javascript.ClassShutter;

import java.util.Set;

public final class SandboxClassShutter implements ClassShutter {

    private static final Set<String> ALLOWLIST = Set.of(
            
            "java.lang.Class",
            "java.lang.String",

            "java.util.List",
            "java.util.ArrayList",

            "com.tendoarisu.dglab.api.ChannelType",
            "com.tendoarisu.dglab.api.ConnectionManager",
            "com.tendoarisu.dglab.core.DgLabMessage",
            "com.tendoarisu.dglab.core.DgLabMessageType",
            "com.tendoarisu.dglab.script.util.DgLabPulseUtil",

            "com.tendoarisu.dglab.script.api.EntityEvents",
            "com.tendoarisu.dglab.script.api.ScriptLogger",
            "com.tendoarisu.dglab.script.wrappers.ConnectionWrapper",
            "com.tendoarisu.dglab.script.wrappers.StrengthWrapper",
            "com.tendoarisu.dglab.script.wrappers.PlayerWrapper",
            "com.tendoarisu.dglab.script.events.PlayerDeathEvent",
            "com.tendoarisu.dglab.script.events.PlayerAfterHurtEvent"
    );

    @Override
    public boolean visibleToScripts(String fullClassName) {
        if (fullClassName == null) return false;
        return ALLOWLIST.contains(fullClassName);
    }
}
