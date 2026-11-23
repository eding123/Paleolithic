package dev.paleolithic.core.creature;

import dev.paleolithic.core.Paleolithic;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Finds Paleolithic creatures as entities load or spawn and gives them their
 * behaviour. Work is queued and done at the end of the server tick, never
 * inside a platform's entity-load callback.
 *
 * <p>Until natural spawning exists, a herd is placed with a vanilla command:
 * {@code /summon ravager ~ ~ ~ {Tags:["paleolithic.spawn.mammoth_herd"]}}.
 * The marker is replaced by a herd of 3-6 mammoths with 1-2 calves.
 */
public final class Creatures {

    public static final String SPAWN_MAMMOTH_HERD = "paleolithic.spawn.mammoth_herd";

    private final Deque<Entity> pending = new ArrayDeque<>();

    /** Platform hook: an entity was added to a server level (loaded or spawned). */
    public void entityLoaded(Entity entity) {
        if (entity instanceof Ravager && !entity.level().isClientSide()) pending.add(entity);
    }

    public void tick(MinecraftServer server) {
        for (int i = pending.size(); i > 0; i--) {
            Entity e = pending.poll();
            if (e == null || e.isRemoved() || !(e instanceof Ravager r) || !(e.level() instanceof ServerLevel level)) {
                continue;
            }
            try {
                setUp(level, r);
            } catch (RuntimeException ex) {
                Paleolithic.LOG.error("Could not set up creature {}", e, ex);
            }
        }
        if (server.getTickCount() % 200 == 0) {
            Herds.prune(server.overworld().getGameTime());
        }
    }

    private void setUp(ServerLevel level, Ravager r) {
        if (r.entityTags().contains(SPAWN_MAMMOTH_HERD)) {
            BlockPos at = r.blockPosition();
            r.discard();
            spawnHerd(level, at, level.getRandom());
        } else if (r.entityTags().contains(Species.MAMMOTH_CALF.tag())) {
            MammothSetup.apply(r, Species.MAMMOTH_CALF);
        } else if (r.entityTags().contains(Species.MAMMOTH.tag())) {
            MammothSetup.apply(r, Species.MAMMOTH);
        }
    }

    /** Spawns a herd of 3-6 including 1-2 calves (Bestiary group size). */
    public static int spawnHerd(ServerLevel level, BlockPos centre, RandomSource random) {
        int size = 3 + random.nextInt(4);
        int calves = 1 + random.nextInt(2);
        String herd = UUID.randomUUID().toString().substring(0, 8);
        BlockPos home = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centre);
        int spawned = 0;
        for (int i = 0; i < size; i++) {
            Species species = i >= size - calves ? Species.MAMMOTH_CALF : Species.MAMMOTH;
            Ravager r = EntityTypes.RAVAGER.create(level, EntitySpawnReason.EVENT);
            if (r == null) continue;
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = i == 0 ? 0 : 4 + random.nextDouble() * 6;
            BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    centre.offset((int) Math.round(Math.cos(angle) * dist), 0, (int) Math.round(Math.sin(angle) * dist)));
            r.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360, 0);
            r.addTag(species.tag());
            r.addTag(Herds.HERD_PREFIX + herd);
            r.addTag(Herds.homeTag(home));
            if (i == 0) r.addTag(Herds.LEADER);
            MammothSetup.apply(r, species);
            if (level.addFreshEntity(r)) spawned++;
        }
        Paleolithic.LOG.info("Spawned a mammoth herd of {} ({} calves) at {}", spawned, calves, home.toShortString());
        return spawned;
    }
}
