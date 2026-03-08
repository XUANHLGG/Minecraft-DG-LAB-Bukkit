package com.tendoarisu.dglab.script.api;

import org.bukkit.plugin.java.JavaPlugin;

public final class ScriptLogger {
    private final JavaPlugin plugin;

    public ScriptLogger(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void log(Object msg) {
        plugin.getLogger().info(String.valueOf(msg));
    }

    public void info(Object msg) {
        plugin.getLogger().info(String.valueOf(msg));
    }

    public void warn(Object msg) {
        plugin.getLogger().warning(String.valueOf(msg));
    }

    public void error(Object msg) {
        plugin.getLogger().severe(String.valueOf(msg));
    }
}
