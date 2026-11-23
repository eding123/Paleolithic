package dev.paleolithic.fabric;

import dev.paleolithic.core.Paleolithic;
import dev.paleolithic.core.platform.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Fabric (and Quilt) adapter: forwards Fabric API events to core. */
public final class PaleolithicFabric implements ModInitializer, Platform {

    @Override
    public String name() {
        return "Fabric";
    }

    @Override
    public void onInitialize() {
        Paleolithic core = new Paleolithic(this);
        ServerLifecycleEvents.SERVER_STARTED.register(core::serverStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(core::serverStopping);
        ServerTickEvents.END_SERVER_TICK.register(core::serverTick);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> core.playerJoined(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> core.playerLeft(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // alive is true when returning from the End: not a death.
            if (!alive) core.playerRespawnedAfterDeath(newPlayer);
        });
    }
}
