package dev.paleolithic.core.heat;

import dev.paleolithic.core.Paleolithic;
import dev.paleolithic.core.data.PlayerStore;
import dev.paleolithic.core.hud.StatusLine;
import dev.paleolithic.rules.Difficulty;
import dev.paleolithic.rules.heat.BiomeClimate;
import dev.paleolithic.rules.heat.BodyHeat;
import dev.paleolithic.rules.heat.FeltTemperature;
import dev.paleolithic.rules.heat.HeatState;
import dev.paleolithic.rules.heat.Surroundings;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Runs the body-heat loop once per second for every player (spec section 5). */
public final class HeatSystem {

    public static final int STEP_TICKS = 20;
    /** Start the server with -Dpaleolithic.debugHeat=true to log every heat step. */
    static final boolean DEBUG = Boolean.getBoolean("paleolithic.debugHeat");
    /** Cold effects last a little longer than one step so they never flicker. */
    static final int EFFECT_TICKS = STEP_TICKS * 2 + 5;

    private final HeatSampler sampler = new HeatSampler(BiomeClimate.defaults());
    private final Map<UUID, PlayerHeat> players = new HashMap<>();
    private final PlayerStore store;

    public HeatSystem(PlayerStore store) {
        this.store = store;
    }

    public PlayerHeat get(ServerPlayer player) {
        return players.get(player.getUUID());
    }

    public void join(ServerPlayer player) {
        PlayerHeat heat = new PlayerHeat();
        store.load(player.getUUID(), "heat").ifPresent(heat::load);
        // Spread players across the second so their steps do not all land on one tick.
        heat.ticksUntilStep = 1 + Math.floorMod(player.getUUID().hashCode(), STEP_TICKS);
        players.put(player.getUUID(), heat);
    }

    public void leave(ServerPlayer player) {
        PlayerHeat heat = players.remove(player.getUUID());
        if (heat != null) store.save(player.getUUID(), "heat", heat.save());
    }

    /** After a death respawn: B = 75 and 30 seconds of Second Wind. */
    public void respawnAfterDeath(ServerPlayer player) {
        PlayerHeat heat = players.get(player.getUUID());
        if (heat == null) return;
        heat.body.respawn();
        heat.wet.load(Integer.MAX_VALUE / 2); // respawn dry
        heat.ticksUntilStep = 1;
    }

    public void saveAll() {
        players.forEach((id, heat) -> store.save(id, "heat", heat.save()));
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PlayerHeat heat = players.get(player.getUUID());
            if (heat == null || --heat.ticksUntilStep > 0) continue;
            heat.ticksUntilStep = STEP_TICKS;
            step(player, heat);
        }
    }

    void step(ServerPlayer player, PlayerHeat heat) {
        if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
            // Creative and spectator players do not feel the cold, but keep the HUD.
            if (DEBUG) {
                Paleolithic.LOG.info("heat {}: paused (spectator={} creative={} alive={})",
                        player.getPlainTextName(), player.isSpectator(), player.isCreative(), player.isAlive());
            }
            if (heat.last != null) StatusLine.show(player, heat.last);
            return;
        }
        Surroundings s = sampler.sample(player, heat.wet, STEP_TICKS);
        FeltTemperature.Breakdown t = FeltTemperature.compute(s);
        BodyHeat.StepResult r = heat.body.step(t.total(), t.environment(), difficulty(player), 1.0, STEP_TICKS);
        heat.last = r;
        heat.lastBreakdown = t;
        if (DEBUG) {
            Paleolithic.LOG.info("heat {}: B={} rate={} T={} {} secondWind={} surroundings={}",
                    player.getPlainTextName(), String.format("%.2f", r.bodyHeat()),
                    String.format("%.3f", r.ratePerSecond()), String.format("%.1f", t.total()), t,
                    heat.body.secondWindTicks(), s);
        }
        applyEffects(player, r);
        StatusLine.show(player, r);
    }

    static void applyEffects(ServerPlayer player, BodyHeat.StepResult r) {
        HeatState state = r.state();
        refresh(player, MobEffects.SLOWNESS, state.slownessAmplifier());
        refresh(player, MobEffects.MINING_FATIGUE, state.miningFatigueAmplifier());
        if (r.damagePoints() > 0) {
            player.hurtServer(player.level(), player.damageSources().freeze(), r.damagePoints());
        }
    }

    private static void refresh(ServerPlayer player, Holder<MobEffect> effect, int amplifier) {
        if (amplifier < 0) return;
        // Ambient, no particles, no icon: the status line is the indicator.
        player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier, true, false, false));
    }

    static Difficulty difficulty(ServerPlayer player) {
        return switch (player.level().getDifficulty()) {
            case PEACEFUL -> Difficulty.PEACEFUL;
            case EASY -> Difficulty.EASY;
            case NORMAL -> Difficulty.NORMAL;
            case HARD -> Difficulty.HARD;
        };
    }
}
