package dev.paleolithic.paper;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import dev.paleolithic.core.Paleolithic;
import dev.paleolithic.core.platform.Platform;
import dev.paleolithic.paper.internal.Handles;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

/** Paper (and Purpur, Pufferfish) adapter: forwards Bukkit events to core. */
public final class PaleolithicPaper extends JavaPlugin implements Platform, Listener {

    private Paleolithic core;

    @Override
    public String name() {
        return "Paper";
    }

    @Override
    public void onEnable() {
        core = new Paleolithic(this);
        // Paper enables plugins after the worlds load, so the server counts as started here.
        core.serverStarted(Handles.server());
        getServer().getPluginManager().registerEvents(this, this);
        // Runs at the start of every tick on the main thread, the same thread
        // vanilla ticks players on.
        getServer().getScheduler().runTaskTimer(this, () -> core.serverTick(Handles.server()), 1, 1);
    }

    @Override
    public void onDisable() {
        if (core != null) core.serverStopping(Handles.server());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityAdded(EntityAddToWorldEvent e) {
        if (core != null) core.entityLoaded(Handles.entity(e.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        core.playerJoined(Handles.player(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        core.playerLeft(Handles.player(e.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerPostRespawnEvent e) {
        if (e.getRespawnReason() == PlayerRespawnEvent.RespawnReason.DEATH) {
            core.playerRespawnedAfterDeath(Handles.player(e.getPlayer()));
        }
    }
}
