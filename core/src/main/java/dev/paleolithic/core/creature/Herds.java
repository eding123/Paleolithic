package dev.paleolithic.core.creature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Herd membership and shared herd mood. Membership and the home point live
 * in each member's scoreboard tags, so they are saved with the entities;
 * anger and flight are short-lived and kept in memory only.
 */
public final class Herds {

    public static final String HERD_PREFIX = "paleolithic.herd.";
    public static final String HOME_PREFIX = "paleolithic.home.";
    public static final String LEADER = "paleolithic.leader";

    /** Herd members only look for each other this far away. */
    static final double SEARCH = 48;

    static final class Mood {
        UUID attacker;
        long angryUntil;
        /** Adults that joined the defence: those within 24 blocks of the animal that raised the alarm. */
        final java.util.Set<UUID> defenders = new java.util.HashSet<>();
        long fleeingUntil;
        Vec3 fleeFrom;
    }

    private static final Map<String, Mood> MOODS = new HashMap<>();

    private Herds() {}

    static String herdOf(Mob mob) {
        for (String t : mob.entityTags()) if (t.startsWith(HERD_PREFIX)) return t.substring(HERD_PREFIX.length());
        return null;
    }

    static BlockPos homeOf(Mob mob) {
        for (String t : mob.entityTags()) {
            if (!t.startsWith(HOME_PREFIX)) continue;
            String[] p = t.substring(HOME_PREFIX.length()).split("_");
            try {
                return new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
            } catch (RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    static String homeTag(BlockPos pos) {
        return HOME_PREFIX + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
    }

    static List<Mob> members(Mob mob, String herd, double radius) {
        if (herd == null || !(mob.level() instanceof ServerLevel level)) return List.of(mob);
        String tag = HERD_PREFIX + herd;
        return level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius),
                m -> m.isAlive() && m.entityTags().contains(tag));
    }

    static Mood mood(String herd) {
        return herd == null ? new Mood() : MOODS.computeIfAbsent(herd, h -> new Mood());
    }

    /**
     * Called by any member that is hurt or sees a player too close to a calf.
     * Adults within {@code radius} of that member defend it.
     */
    static void provoke(Mob alarmed, String herd, UUID attacker, long now, double radius) {
        Mood m = mood(herd);
        if (now < m.fleeingUntil) return;
        if (!attacker.equals(m.attacker) || now >= m.angryUntil) m.defenders.clear();
        m.attacker = attacker;
        m.angryUntil = now + MammothBrain.ANGER_TICKS;
        for (Mob member : members(alarmed, herd, radius)) {
            if (member.entityTags().contains(Species.MAMMOTH_CALF.tag()) || member.distanceTo(alarmed) > radius) continue;
            m.defenders.add(member.getUUID());
        }
    }

    static void flee(String herd, Vec3 from, long now) {
        Mood m = mood(herd);
        m.attacker = null;
        m.angryUntil = 0;
        m.defenders.clear();
        m.fleeFrom = from;
        m.fleeingUntil = now + MammothBrain.FLEE_TICKS;
    }

    static void calm(String herd) {
        Mood m = mood(herd);
        m.attacker = null;
        m.angryUntil = 0;
        m.defenders.clear();
    }

    /** A player died: no herd keeps hunting them after they respawn (spec rule 4, fail forward). */
    public static void forgive(UUID player) {
        for (Mood m : MOODS.values()) {
            if (player.equals(m.attacker)) {
                m.attacker = null;
                m.angryUntil = 0;
                m.defenders.clear();
            }
        }
    }

    /** Forget herds whose mood has expired, so the map stays small. */
    public static void prune(long now) {
        MOODS.values().removeIf(m -> m.angryUntil < now && m.fleeingUntil < now);
    }
}
