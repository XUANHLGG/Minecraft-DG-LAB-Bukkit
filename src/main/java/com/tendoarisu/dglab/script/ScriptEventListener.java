package com.tendoarisu.dglab.script;

import com.tendoarisu.dglab.script.events.EventType;
import com.tendoarisu.dglab.script.events.PlayerAfterHurtEvent;
import com.tendoarisu.dglab.script.events.PlayerDeathEvent;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class ScriptEventListener implements Listener {

    private final ScriptRuntime runtime;

    public ScriptEventListener(ScriptRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player p = event.getEntity();
        if (p == null) return;
        runtime.fire(EventType.PLAYER_DEATH, new PlayerDeathEvent(p));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        runtime.fire(EventType.PLAYER_AFTER_HURT, new PlayerAfterHurtEvent(player, event.getFinalDamage()));
    }
}
