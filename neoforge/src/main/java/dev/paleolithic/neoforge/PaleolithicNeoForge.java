package dev.paleolithic.neoforge;

import dev.paleolithic.core.Paleolithic;
import dev.paleolithic.core.platform.Platform;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** NeoForge adapter: forwards NeoForge game events to core. */
@Mod(Paleolithic.MOD_ID)
public final class PaleolithicNeoForge implements Platform {

    private final Paleolithic core;

    public PaleolithicNeoForge() {
        core = new Paleolithic(this);
        var bus = NeoForge.EVENT_BUS;
        bus.addListener((ServerStartedEvent e) -> core.serverStarted(e.getServer()));
        bus.addListener((ServerStoppingEvent e) -> core.serverStopping(e.getServer()));
        bus.addListener((ServerTickEvent.Post e) -> core.serverTick(e.getServer()));
        bus.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) core.playerJoined(p);
        });
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) core.playerLeft(p);
        });
        bus.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (!e.isEndConquered() && e.getEntity() instanceof ServerPlayer p) core.playerRespawnedAfterDeath(p);
        });
    }

    @Override
    public String name() {
        return "NeoForge";
    }
}
