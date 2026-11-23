package dev.paleolithic.core.creature;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * The whole woolly mammoth behaviour (Bestiary, "Woolly mammoth"; spec
 * section 12, Defensive temperament) as one goal that owns movement, looking
 * and jumping. Runs every tick on the server.
 *
 * <ul>
 *   <li>Grazes: the leader roams within 64 blocks of the herd's home, other
 *       adults stay near the leader, calves stay within 6 blocks of an adult.</li>
 *   <li>Trumpets every 1-3 minutes, audible at 96 blocks.</li>
 *   <li>Alert at R (head turns to the player).</li>
 *   <li>Defends when a member is hurt or a player comes within 0.4 R of a calf:
 *       adults within 24 blocks attack for 30 s or until the player is 32 away.</li>
 *   <li>Tusk sweep: 14 damage in a 3-block arc after a 0.8 s head raise.
 *       Charge: 10 blocks at 6.0 after a 1.0 s trumpet and foot paw.</li>
 *   <li>The herd flees once the attacked mammoth is below 25% health.</li>
 * </ul>
 */
final class MammothBrain extends Goal {

    static final int ANGER_TICKS = 30 * 20;
    static final int FLEE_TICKS = 20 * 20;
    static final double DEFEND_RADIUS = 24;
    static final double GIVE_UP_DISTANCE = 32;
    static final double CALF_ALARM = 0.4;
    static final double FLEE_BELOW = 0.25;
    static final double ROAM_RADIUS = 64;
    static final double CALF_LEASH = 6;
    static final double ADULT_LEASH = 12;
    static final int TRUMPET_MIN = 60 * 20;
    static final int TRUMPET_MAX = 180 * 20;
    /** A sound at volume v is heard out to 16 v blocks: 6 gives the spec's 96. */
    static final float TRUMPET_VOLUME = 6;

    static final int SWEEP_TELL = 16;       // 0.8 s
    static final double SWEEP_REACH = 3;    // blocks beyond the body
    static final double SWEEP_ARC_DEGREES = 120;
    static final int SWEEP_COOLDOWN = 30;
    static final int CHARGE_TELL = 20;      // 1.0 s
    static final double CHARGE_DISTANCE = 10;
    static final double CHARGE_MIN = 7;
    static final double CHARGE_MAX = 18;
    static final int CHARGE_COOLDOWN = 8 * 20;
    static final int CHARGE_MAX_TICKS = 50;

    /**
     * A walking mob's top ground speed is K * s^2 blocks per second, where s is
     * attribute x modifier: the move control feeds s in as both the speed and
     * the input, and ground friction keeps 0.546 of the velocity each tick, so
     * K = 20 / (1 - 0.546) = 44.05.
     */
    static final double GROUND_SPEED_K = 20 / (1 - 0.6 * 0.91);

    private enum Action { NONE, SWEEP_TELL, CHARGE_TELL, CHARGING }

    private final PathfinderMob mob;
    private final Species species;

    private int trumpetIn;
    private int wanderIn;
    private Action action = Action.NONE;
    private int actionTicks;
    private int sweepCooldown;
    private int chargeCooldown;
    private Vec3 chargeStart;
    private Vec3 chargeDir;
    private boolean chargeHit;
    private int lastSeenHurt;
    private int repathIn;
    private Mob anchor;
    private int anchorIn;

    MammothBrain(PathfinderMob mob, Species species) {
        this.mob = mob;
        this.species = species;
        // No JUMP flag: FloatGoal keeps it swimming while this goal keeps reacting.
        setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        this.trumpetIn = randomTrumpet();
        this.lastSeenHurt = mob.getLastHurtByMobTimestamp();
    }

    @Override public boolean canUse() { return mob.isAlive(); }
    @Override public boolean canContinueToUse() { return mob.isAlive(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    static final boolean DEBUG = Boolean.getBoolean("paleolithic.debugCreatures");
    private Vec3 debugLast;

    @Override
    public void tick() {
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (DEBUG && mob.tickCount % 20 == 0) {
            double moved = debugLast == null ? 0 : mob.position().subtract(debugLast).horizontalDistance();
            debugLast = mob.position();
            Mob adult = species.isCalf() ? nearestAdult(Herds.herdOf(mob)) : null;
            Herds.Mood dm = Herds.mood(Herds.herdOf(mob));
            dev.paleolithic.core.Paleolithic.LOG.info("{} #{} action={} speed={} b/s health={} angry={} defenders={} defending={}{}",
                    species, mob.getId(), action, String.format("%.2f", moved), mob.getHealth(),
                    dm.attacker != null && level.getGameTime() < dm.angryUntil, dm.defenders.size(),
                    dm.defenders.contains(mob.getUUID()),
                    adult == null ? "" : String.format(" adult=%.1f", mob.distanceTo(adult)));
        }
        long now = level.getGameTime();
        String herd = Herds.herdOf(mob);
        Herds.Mood mood = Herds.mood(herd);

        noticeBeingHurt(level, herd, now);
        if (species.isCalf()) checkCalfAlarm(level, herd, now);
        if (sweepCooldown > 0) sweepCooldown--;
        if (chargeCooldown > 0) chargeCooldown--;

        if (now < mood.fleeingUntil && mood.fleeFrom != null) {
            action = Action.NONE;
            flee(mood.fleeFrom);
            return;
        }

        ServerPlayer target = species.isCalf() ? null : angerTarget(level, herd, mood, now);
        if (target != null) {
            fight(level, target);
            return;
        }
        action = Action.NONE;
        graze(level, herd);
        if (!species.isCalf() && --trumpetIn <= 0) {
            trumpet(level, 1.0f);
            trumpetIn = randomTrumpet();
        }
        lookAtWatcher(level);
    }

    // --- reacting ---------------------------------------------------------

    private void noticeBeingHurt(ServerLevel level, String herd, long now) {
        int hurtAt = mob.getLastHurtByMobTimestamp();
        if (hurtAt == lastSeenHurt) return;
        lastSeenHurt = hurtAt;
        if (!(mob.getLastHurtByMob() instanceof Player attacker)) return;
        if (mob.getHealth() < mob.getMaxHealth() * FLEE_BELOW) {
            Herds.flee(herd, attacker.position(), now);
        } else {
            Herds.provoke(mob, herd, attacker.getUUID(), now, DEFEND_RADIUS);
        }
    }

    private void checkCalfAlarm(ServerLevel level, String herd, long now) {
        double r = MammothSetup.detectionRadius(level, species);
        Player p = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), r * CALF_ALARM, true);
        if (p instanceof ServerPlayer sp && isHuntable(sp)) Herds.provoke(mob, herd, p.getUUID(), now, DEFEND_RADIUS);
    }

    private ServerPlayer angerTarget(ServerLevel level, String herd, Herds.Mood mood, long now) {
        if (mood.attacker == null || now >= mood.angryUntil) return null;
        if (!mood.defenders.contains(mob.getUUID())) return null;
        if (!(level.getPlayerByUUID(mood.attacker) instanceof ServerPlayer p) || !isHuntable(p)) {
            Herds.calm(herd);
            return null;
        }
        if (mob.distanceTo(p) > GIVE_UP_DISTANCE) {
            // This adult gives up; the others keep going until their own limit.
            mood.defenders.remove(mob.getUUID());
            return null;
        }
        return p;
    }

    static boolean isHuntable(ServerPlayer p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    // --- fighting ---------------------------------------------------------

    private void fight(ServerLevel level, ServerPlayer target) {
        double gap = horizontalGap(target);
        switch (action) {
            case SWEEP_TELL -> {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(target, 30, 30);
                if (++actionTicks >= SWEEP_TELL) {
                    sweep(level);
                    action = Action.NONE;
                    sweepCooldown = SWEEP_COOLDOWN;
                }
            }
            case CHARGE_TELL -> {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(target, 30, 30);
                if (actionTicks % 5 == 0) paw(level);
                if (++actionTicks >= CHARGE_TELL) startCharge(target);
            }
            case CHARGING -> charge(level);
            case NONE -> {
                if (gap <= SWEEP_REACH && sweepCooldown == 0) {
                    beginSweepTell(level);
                } else if (gap >= CHARGE_MIN && gap <= CHARGE_MAX && chargeCooldown == 0 && mob.onGround()) {
                    action = Action.CHARGE_TELL;
                    actionTicks = 0;
                    trumpet(level, 1.2f);
                } else {
                    // Re-plan the path a few times a second, as vanilla's melee goal does.
                    if (mob.getNavigation().isDone() || --repathIn <= 0) {
                        mob.getNavigation().moveTo(target, speed(species.runSpeed));
                        repathIn = 10;
                    }
                    mob.getLookControl().setLookAt(target, 30, 30);
                }
            }
        }
    }

    private void beginSweepTell(ServerLevel level) {
        action = Action.SWEEP_TELL;
        actionTicks = 0;
        // The ravager's head-swing animation is the visible tell on a vanilla client.
        level.broadcastEntityEvent(mob, (byte) 4);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE,
                1.5f, 0.7f);
    }

    private void sweep(ServerLevel level) {
        Vec3 facing = Vec3.directionFromRotation(0, mob.getYHeadRot());
        double halfArc = Math.toRadians(SWEEP_ARC_DEGREES / 2);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(SWEEP_REACH))) {
            if (!isHuntable(p) || horizontalGap(p) > SWEEP_REACH) continue;
            Vec3 to = new Vec3(p.getX() - mob.getX(), 0, p.getZ() - mob.getZ());
            if (to.lengthSqr() > 1e-6 && Math.acos(Mth.clamp(to.normalize().dot(facing), -1, 1)) > halfArc) continue;
            hit(level, p, 1.0);
        }
    }

    private void startCharge(ServerPlayer target) {
        Vec3 to = new Vec3(target.getX() - mob.getX(), 0, target.getZ() - mob.getZ());
        if (to.lengthSqr() < 1e-6) {
            action = Action.NONE;
            return;
        }
        chargeDir = to.normalize();
        chargeStart = mob.position();
        chargeHit = false;
        action = Action.CHARGING;
        actionTicks = 0;
    }

    private void charge(ServerLevel level) {
        actionTicks++;
        Vec3 goal = chargeStart.add(chargeDir.scale(CHARGE_DISTANCE + 2));
        mob.getMoveControl().setWantedPosition(goal.x, mob.getY(), goal.z, speed(species.runSpeed));
        if (!chargeHit) {
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, mob.getBoundingBox().inflate(0.6))) {
                if (!isHuntable(p)) continue;
                hit(level, p, 1.5);
                chargeHit = true;
            }
        }
        double run = mob.position().subtract(chargeStart).horizontalDistance();
        if (run >= CHARGE_DISTANCE || actionTicks >= CHARGE_MAX_TICKS || chargeHit || mob.horizontalCollision) {
            action = Action.NONE;
            chargeCooldown = CHARGE_COOLDOWN;
            mob.getNavigation().stop();
        }
    }

    private void hit(ServerLevel level, ServerPlayer p, double knockback) {
        p.hurtServer(level, mob.damageSources().mobAttack(mob), (float) species.damage);
        Vec3 push = new Vec3(p.getX() - mob.getX(), 0, p.getZ() - mob.getZ());
        if (push.lengthSqr() > 1e-6) {
            push = push.normalize().scale(knockback);
            p.push(push.x, 0.4, push.z);
            p.syncVelocity = true;
        }
    }

    /** Distance from the edge of the body to the player, ignoring height. */
    private double horizontalGap(ServerPlayer p) {
        double centre = Math.hypot(p.getX() - mob.getX(), p.getZ() - mob.getZ());
        return centre - mob.getBbWidth() / 2 - p.getBbWidth() / 2;
    }

    private void paw(ServerLevel level) {
        BlockPos below = mob.blockPosition().below();
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)),
                mob.getX(), mob.getY() + 0.1, mob.getZ(), 12, mob.getBbWidth() / 3, 0.05, mob.getBbWidth() / 3, 0.1);
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE,
                1.5f, 0.6f);
    }

    // --- peaceful behaviour -----------------------------------------------

    private void flee(Vec3 from) {
        if (mob.getNavigation().isDone() || mob.getRandom().nextInt(20) == 0) {
            Vec3 away = LandRandomPos.getPosAway(mob, 24, 7, from);
            if (away != null) mob.getNavigation().moveTo(away.x, away.y, away.z, speed(species.runSpeed));
        }
    }

    private void graze(ServerLevel level, String herd) {
        double leash;
        boolean leader = mob.entityTags().contains(Herds.LEADER);
        if (!leader && (--anchorIn <= 0 || anchor == null || !anchor.isAlive())) {
            // Looking for the mother or leader is an area scan; refresh it once a second.
            anchor = species.isCalf() ? nearestAdult(herd) : leader(herd);
            anchorIn = 20;
        }
        if (leader) {
            anchor = null;
            leash = ROAM_RADIUS;
        } else {
            leash = species.isCalf() ? CALF_LEASH : ADULT_LEASH;
        }
        Mob anchor = this.anchor;

        if (anchor != null && mob.distanceTo(anchor) > leash - 2) {
            // Too far from mother or leader: walk back, a little faster than grazing.
            if (mob.getNavigation().isDone() || mob.tickCount % 20 == 0) {
                mob.getNavigation().moveTo(anchor, speed(species.walkSpeed * 1.3));
            }
            return;
        }
        if (--wanderIn > 0 || !mob.getNavigation().isDone()) return;
        wanderIn = 120 + mob.getRandom().nextInt(160);

        Vec3 target;
        if (anchor != null) {
            target = LandRandomPos.getPos(mob, (int) leash / 2, 4);
            if (target != null && target.distanceTo(anchor.position()) > leash - 2) target = null;
        } else {
            BlockPos home = Herds.homeOf(mob);
            if (home != null && mob.position().distanceTo(Vec3.atBottomCenterOf(home)) > ROAM_RADIUS - 16) {
                target = LandRandomPos.getPosTowards(mob, 16, 7, Vec3.atBottomCenterOf(home));
            } else {
                target = LandRandomPos.getPos(mob, 16, 7);
            }
        }
        if (target != null) mob.getNavigation().moveTo(target.x, target.y, target.z, speed(species.walkSpeed));
    }

    private void lookAtWatcher(ServerLevel level) {
        double r = MammothSetup.detectionRadius(level, species);
        Player p = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), r, true);
        if (p instanceof ServerPlayer sp && isHuntable(sp) && mob.getNavigation().isDone()) {
            // Alert: the head follows the player who has been noticed.
            double sneak = p.isShiftKeyDown() ? 0.5 : 1.0;
            if (mob.distanceTo(p) <= r * sneak) mob.getLookControl().setLookAt(p, 10, 10);
        }
    }

    private void trumpet(ServerLevel level, float pitchScale) {
        float pitch = (species.isCalf() ? 1.2f : 0.55f) * pitchScale;
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.NEUTRAL,
                TRUMPET_VOLUME, pitch);
    }

    private Mob leader(String herd) {
        for (Mob m : Herds.members(mob, herd, Herds.SEARCH)) {
            if (m != mob && m.entityTags().contains(Herds.LEADER)) return m;
        }
        return null;
    }

    private Mob nearestAdult(String herd) {
        Mob best = null;
        double bestD = Double.MAX_VALUE;
        List<Mob> members = Herds.members(mob, herd, Herds.SEARCH);
        for (Mob m : members) {
            if (m == mob || !m.entityTags().contains(Species.MAMMOTH.tag())) continue;
            double d = m.distanceToSqr(mob);
            if (d < bestD) {
                bestD = d;
                best = m;
            }
        }
        return best;
    }

    private int randomTrumpet() {
        return TRUMPET_MIN + mob.getRandom().nextInt(TRUMPET_MAX - TRUMPET_MIN + 1);
    }

    /** Converts blocks per second into a navigation speed modifier for this mob. */
    private double speed(double blocksPerSecond) {
        double attribute = Math.max(1e-3, mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
        return Math.sqrt(blocksPerSecond / GROUND_SPEED_K) / attribute;
    }
}
