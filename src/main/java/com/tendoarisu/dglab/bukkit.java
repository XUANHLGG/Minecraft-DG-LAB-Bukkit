package com.tendoarisu.dglab;

import com.tendoarisu.dglab.command.DgLabCommandExecutor;
import com.tendoarisu.dglab.core.DgLabConnectionManager;
import com.tendoarisu.dglab.proto.DgLabChannels;
import com.tendoarisu.dglab.proto.CustomPayloadSender;
import com.tendoarisu.dglab.script.ScriptEventListener;
import com.tendoarisu.dglab.script.ScriptManager;
import com.tendoarisu.dglab.script.ScriptRuntime;
import com.tendoarisu.dglab.ws.DgLabWebSocketServer;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;

public final class bukkit extends JavaPlugin implements Listener {

    private DgLabWebSocketServer webSocketServer;

    private ScriptRuntime scriptRuntime;
    private ScriptManager scriptManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Bukkit.getMessenger().registerOutgoingPluginChannel(this, DgLabChannels.STRENGTH);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, DgLabChannels.CLEAR_STRENGTH);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, DgLabChannels.SHOW_QR_CODE);

        scriptRuntime = new ScriptRuntime(this);
        scriptManager = new ScriptManager(this, scriptRuntime);
        Bukkit.getPluginManager().registerEvents(new ScriptEventListener(scriptRuntime), this);
        scriptManager.reloadAll();

        if (getCommand("dglab") != null) {
            DgLabCommandExecutor executor = new DgLabCommandExecutor(this);
            getCommand("dglab").setExecutor(executor);
            getCommand("dglab").setTabCompleter(executor);
        }

        Bukkit.getPluginManager().registerEvents(this, this);

        if (getConfig().getBoolean("websocket.enabled", false)) {
            startWebSocketServer();
        }

        getLogger().info("DG-LAB Bukkit 已启用");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Listener) this);

        stopWebSocketServer();

        DgLabConnectionManager.disconnectAll();

        getLogger().info("DG-LAB Bukkit 已禁用");
    }

    @org.bukkit.event.EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        
        UUID u = event.getPlayer().getUniqueId();
        DgLabConnectionManager.disconnectByPlayer(u);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        
        UUID u = event.getPlayer().getUniqueId();
        DgLabConnectionManager.disconnectByPlayer(u);
    }

    public void reloadPlugin() {
        reloadConfig();

        if (scriptManager != null) {
            scriptManager.reloadAll();
        }

        boolean enabled = getConfig().getBoolean("websocket.enabled", false);
        if (enabled) {
            if (webSocketServer == null || !webSocketServer.isRunning()) {
                startWebSocketServer();
            } else {
                
                stopWebSocketServer();
                startWebSocketServer();
            }
        } else {
            stopWebSocketServer();
        }
    }

    private void startWebSocketServer() {
        String address = getConfig().getString("websocket.address", "127.0.0.1");
        int port = getConfig().getInt("websocket.port", 8080);

        webSocketServer = new DgLabWebSocketServer(this, address, port);
        webSocketServer.start();
    }

    private void stopWebSocketServer() {
        if (webSocketServer != null) {
            webSocketServer.stop();
            webSocketServer = null;
        }
    }

    public String buildQrUrl(UUID playerUuid) {
        boolean useHttps = getConfig().getBoolean("websocket.useHttps", false);
        String address = getConfig().getString("websocket.address", "127.0.0.1");
        int port = getConfig().getInt("websocket.port", 8080);

        String schema = useHttps ? "wss" : "ws";
        return "https://www.dungeon-lab.com/app-download.php#DGLAB-SOCKET#%s://%s:%d/%s"
                .formatted(schema, address, port, playerUuid);
    }

    public boolean isWebSocketRunning() {
        return webSocketServer != null && webSocketServer.isRunning();
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }
}
