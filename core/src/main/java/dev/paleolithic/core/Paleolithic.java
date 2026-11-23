package dev.paleolithic.core;

import dev.paleolithic.core.creature.Creatures;
import dev.paleolithic.core.data.PlayerStore;
import dev.paleolithic.core.heat.HeatSystem;
import dev.paleolithic.core.platform.Platform;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the shared game code. Each platform adapter creates one
 * instance and forwards its lifecycle and player events here; everything
 * else is plain vanilla code that runs the same on every platform.
 */
public final class Paleolithic {

    public static final String MOD_ID = "paleolithic";
    public static final Logger LOG = LoggerFactory.getLogger("Paleolithic");

    private final Platform platform;
    private HeatSystem heat;
    private final Creatures creatures = new Creatures();

    public Paleolithic(Platform platform) {
        this.platform = platform;
        LOG.info("Paleolithic loaded on {}", platform.name());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public Platform platform() { return platform; }

    /** Null until the server has started. */
    public HeatSystem heat() { return heat; }

    public void serverStarted(MinecraftServer server) {
        heat = new HeatSystem(new PlayerStore(server));
        // Players already online (for example after a plugin reload) join now.
        server.getPlayerList().getPlayers().forEach(heat::join);
        // Creatures already in loaded chunks (for example after a plugin reload) get their AI too.
        for (var level : server.getAllLevels()) level.getAllEntities().forEach(creatures::entityLoaded);
        LOG.info("Paleolithic body heat running on {}", platform.name());
    }

    public void serverStopping(MinecraftServer server) {
        if (heat != null) heat.saveAll();
        heat = null;
    }

    public void serverTick(MinecraftServer server) {
        if (heat != null) heat.tick(server);
        creatures.tick(server);
    }

    /** An entity was added to a server level, by chunk loading or by spawning. */
    public void entityLoaded(Entity entity) {
        creatures.entityLoaded(entity);
    }

    public void playerJoined(ServerPlayer player) {
        if (heat != null) heat.join(player);
    }

    public void playerLeft(ServerPlayer player) {
        if (heat != null) heat.leave(player);
    }

    /** Call only for a respawn after death, not for leaving the End. */
    public void playerRespawnedAfterDeath(ServerPlayer player) {
        if (heat != null) heat.respawnAfterDeath(player);
        dev.paleolithic.core.creature.Herds.forgive(player.getUUID());
    }
}
