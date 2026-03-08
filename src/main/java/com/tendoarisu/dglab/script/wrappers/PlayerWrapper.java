package com.tendoarisu.dglab.script.wrappers;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class PlayerWrapper {
    private final Player player;

    public PlayerWrapper(Player player) {
        this.player = player;
    }

    public String getUuid() {
        return player.getUniqueId().toString();
    }

    public String getUuidString() {
        return getUuid();
    }

    public String getName() {
        return player.getName();
    }

    public boolean isOnline() {
        return player.isOnline();
    }

    public int getHealth() {
        return (int) Math.ceil(player.getHealth());
    }

    public int getFoodLevel() {
        return player.getFoodLevel();
    }

    public String getWorldName() {
        World w = player.getWorld();
        return w == null ? "" : w.getName();
    }

    public double getX() {
        Location l = player.getLocation();
        return l.getX();
    }

    public double getY() {
        Location l = player.getLocation();
        return l.getY();
    }

    public double getZ() {
        Location l = player.getLocation();
        return l.getZ();
    }
}
