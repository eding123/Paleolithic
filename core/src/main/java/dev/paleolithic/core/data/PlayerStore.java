package dev.paleolithic.core.data;

import dev.paleolithic.core.Paleolithic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Per-player Paleolithic data, one compressed NBT file per player in
 * {@code <world>/paleolithic/players/}. Written by core with vanilla NBT, so
 * it behaves the same on every platform and moves with the world folder.
 */
public final class PlayerStore {

    private final Path dir;

    public PlayerStore(MinecraftServer server) {
        this(server.getWorldPath(LevelResource.ROOT).resolve(Paleolithic.MOD_ID).resolve("players"));
    }

    public PlayerStore(Path dir) {
        this.dir = dir;
    }

    public Optional<CompoundTag> load(UUID player, String key) {
        Path file = file(player);
        if (!Files.exists(file)) return Optional.empty();
        try {
            return NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()).getCompound(key);
        } catch (IOException e) {
            Paleolithic.LOG.error("Could not read {}", file, e);
            return Optional.empty();
        }
    }

    public void save(UUID player, String key, CompoundTag value) {
        Path file = file(player);
        try {
            Files.createDirectories(dir);
            CompoundTag root = Files.exists(file)
                    ? NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap()) : new CompoundTag();
            root.put(key, value);
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            NbtIo.writeCompressed(root, tmp);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Paleolithic.LOG.error("Could not write {}", file, e);
        }
    }

    private Path file(UUID player) {
        return dir.resolve(player + ".dat");
    }
}
