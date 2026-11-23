package dev.paleolithic.paper.internal;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 * The only place the Paper adapter touches Paper internals (spec section 20):
 * turning Bukkit objects into the vanilla objects core works with.
 */
public final class Handles {

    private Handles() {}

    public static MinecraftServer server() {
        return ((CraftServer) Bukkit.getServer()).getServer();
    }

    public static net.minecraft.world.entity.Entity entity(org.bukkit.entity.Entity entity) {
        return ((CraftEntity) entity).getHandle();
    }

    public static ServerPlayer player(Player player) {
        return ((CraftPlayer) player).getHandle();
    }
}
