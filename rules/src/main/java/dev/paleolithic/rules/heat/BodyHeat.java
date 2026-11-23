package dev.paleolithic.rules.heat;

import dev.paleolithic.rules.Difficulty;

/**
 * One player's body heat B (0-100) and the timers that hang off it
 * (spec sections 5, 8 and 18). Call {@link #step} once per second.
 */
public final class BodyHeat {

    public static final double MAX = 100;
    public static final double SPAWN_VALUE = 75;
    public static final double COMFORT = 10;
    public static final double LOSS_DIVISOR = 40;
    public static final double MAX_LOSS_PER_SECOND = 1.5;
    public static final double MAX_GAIN_PER_SECOND = 2.0;
    public static final double BASE_GAIN_PER_SECOND = 0.25;
    public static final int SECOND_WIND_TICKS = 30 * 20;

    public static final double HEAT_STRESS_ENV = 20;
    public static final double HEAT_STRESS_FELT = 32;
    public static final double HEAT_STRESS_CLEAR_BELOW = 30;
    public static final int HEAT_STRESS_ONSET_TICKS = 30 * 20;
    public static final int HEAT_STRESS_CLEAR_TICKS = 10 * 20;

    public static final double FAST_COOLING_PER_SECOND = 0.5;

    public enum Trend { WARMING, STABLE, COOLING, COOLING_FAST }

    /** Outcome of one step, for the platform layer to apply. */
    public record StepResult(double bodyHeat, HeatState state, Trend trend, double ratePerSecond,
                             int damagePoints, boolean overheated) {}

    private double value = SPAWN_VALUE;
    private int secondWindTicks;
    private int hypothermiaTicks;
    private int stressTicks;
    private int coolTicks;
    private boolean overheated;

    public double value() { return value; }
    public HeatState state() { return HeatState.of(value); }
    public boolean overheated() { return overheated; }
    public int secondWindTicks() { return secondWindTicks; }

    /** Reset for a respawn: B = 75 and 30 seconds of Second Wind. */
    public void respawn() {
        value = SPAWN_VALUE;
        secondWindTicks = SECOND_WIND_TICKS;
        hypothermiaTicks = 0;
        stressTicks = 0;
        coolTicks = 0;
        overheated = false;
    }

    /** Body-heat change per second at felt temperature {@code t}, before floors and Second Wind. */
    public static double ratePerSecond(double t, Difficulty difficulty, double configMultiplier) {
        if (t < COMFORT) {
            double loss = (COMFORT - t) / LOSS_DIVISOR * difficulty.coldLossMultiplier * configMultiplier;
            // The cap applies after every multiplier, so rule 3's 40-second
            // warning holds on every difficulty and config.
            return -Math.min(MAX_LOSS_PER_SECOND, loss);
        }
        return Math.min(MAX_GAIN_PER_SECOND, BASE_GAIN_PER_SECOND + (t - COMFORT) / LOSS_DIVISOR);
    }

    /**
     * Advance by {@code ticks} (normally 20).
     *
     * @param felt             felt temperature T
     * @param environment      T_env, used only for heat stress
     * @param difficulty       world difficulty
     * @param configMultiplier {@code cold.rateMultiplier} from the server config
     */
    public StepResult step(double felt, double environment, Difficulty difficulty, double configMultiplier,
                           int ticks) {
        double seconds = ticks / 20.0;
        double rate = ratePerSecond(felt, difficulty, configMultiplier);
        if (rate < 0 && secondWindTicks > 0) rate = 0;
        secondWindTicks = Math.max(0, secondWindTicks - ticks);

        value = clamp(value + rate * seconds, difficulty.bodyHeatFloor, MAX);

        int damage = 0;
        if (value <= 0 && difficulty.hypothermiaDamageIntervalTicks > 0) {
            hypothermiaTicks += ticks;
            while (hypothermiaTicks >= difficulty.hypothermiaDamageIntervalTicks) {
                hypothermiaTicks -= difficulty.hypothermiaDamageIntervalTicks;
                damage++;
            }
        } else {
            hypothermiaTicks = 0;
        }

        updateHeatStress(felt, environment, ticks);

        return new StepResult(value, state(), trendOf(rate, value), rate, damage, overheated);
    }

    private void updateHeatStress(double felt, double environment, int ticks) {
        boolean stressing = environment >= HEAT_STRESS_ENV && felt >= HEAT_STRESS_FELT;
        if (!overheated) {
            stressTicks = stressing ? stressTicks + ticks : 0;
            if (stressTicks >= HEAT_STRESS_ONSET_TICKS) {
                overheated = true;
                coolTicks = 0;
            }
        } else {
            coolTicks = felt < HEAT_STRESS_CLEAR_BELOW ? coolTicks + ticks : 0;
            if (coolTicks >= HEAT_STRESS_CLEAR_TICKS) {
                overheated = false;
                stressTicks = 0;
            }
        }
    }

    private static Trend trendOf(double rate, double value) {
        if (rate > 1e-9 && value < MAX) return Trend.WARMING;
        if (rate <= -FAST_COOLING_PER_SECOND) return Trend.COOLING_FAST;
        if (rate < -1e-9) return Trend.COOLING;
        return Trend.STABLE;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // --- persistence -----------------------------------------------------

    public Snapshot save() {
        return new Snapshot(value, secondWindTicks, hypothermiaTicks, stressTicks, coolTicks, overheated);
    }

    public void load(Snapshot s) {
        value = clamp(s.value(), 0, MAX);
        secondWindTicks = s.secondWindTicks();
        hypothermiaTicks = s.hypothermiaTicks();
        stressTicks = s.stressTicks();
        coolTicks = s.coolTicks();
        overheated = s.overheated();
    }

    public record Snapshot(double value, int secondWindTicks, int hypothermiaTicks, int stressTicks,
                           int coolTicks, boolean overheated) {}
}
