package com.tendoarisu.dglab.script.events;

import com.tendoarisu.dglab.script.wrappers.PlayerWrapper;

import org.bukkit.entity.Player;

public final class PlayerDeathEvent {
    private final PlayerWrapper player;

    public PlayerDeathEvent(Player player) {
        this.player = new PlayerWrapper(player);
    }

    public PlayerWrapper getEntity() {
        return player;
    }
}
