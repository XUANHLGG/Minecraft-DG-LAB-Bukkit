package com.tendoarisu.dglab.script.events;

import com.tendoarisu.dglab.script.wrappers.PlayerWrapper;

import org.bukkit.entity.Player;

public final class PlayerAfterHurtEvent {
    private final PlayerWrapper player;
    private final double damage;

    public PlayerAfterHurtEvent(Player player, double damage) {
        this.player = new PlayerWrapper(player);
        this.damage = damage;
    }

    public PlayerWrapper getEntity() {
        return player;
    }

    public double getDamage() {
        return damage;
    }
}
