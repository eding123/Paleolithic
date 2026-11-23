package dev.paleolithic.rules.fire;

import java.util.Map;
import java.util.OptionalInt;

/**
 * Fuel, embers and charcoal for a campfire or hearth (spec section 6).
 * All times are in ticks. Call {@link #tick} with the ticks elapsed.
 */
public final class Fire {

    public enum Kind {
        CAMPFIRE(16 * 60 * 20, 1.0, 5 * 60 * 20),
        HEARTH(40 * 60 * 20, 0.5, 10 * 60 * 20);

        public final int maxFuelTicks;
        public final double burnRate;
        public final int emberTicks;

        Kind(int maxFuelTicks, double burnRate, int emberTicks) {
            this.maxFuelTicks = maxFuelTicks;
            this.burnRate = burnRate;
            this.emberTicks = emberTicks;
        }
    }

    public enum State { COLD, LIT, EMBERS }

    public enum Exposure {
        COVERED(1), PRECIPITATION(2), BLIZZARD(3);

        public final int burnMultiplier;

        Exposure(int burnMultiplier) { this.burnMultiplier = burnMultiplier; }
    }

    public enum Fuel {
        STICK(30), PLANKS(60), LOG(240), ANIMAL_FAT(120), COAL(360);

        public final int ticks;

        Fuel(int seconds) { this.ticks = seconds * 20; }
    }

    public static final int LIGHTING_FUEL_TICKS = 4 * 60 * 20;
    public static final int MAX_CHARCOAL = 4;

    /** Default item-to-fuel mapping; the core module loads overrides from data. */
    public static final Map<String, Fuel> DEFAULT_FUELS = Map.of(
            "minecraft:stick", Fuel.STICK,
            "#minecraft:planks", Fuel.PLANKS,
            "#minecraft:logs_that_burn", Fuel.LOG,
            "paleolithic:animal_fat", Fuel.ANIMAL_FAT,
            "minecraft:coal", Fuel.COAL,
            "minecraft:charcoal", Fuel.COAL);

    private final Kind kind;
    private State state = State.COLD;
    private double fuelTicks;
    private int emberTicksLeft;
    private int logsSinceLit;
    private int charcoalWaiting;

    public Fire(Kind kind) { this.kind = kind; }

    public Kind kind() { return kind; }
    public State state() { return state; }
    public int fuelTicks() { return (int) Math.ceil(fuelTicks); }
    public int emberTicksLeft() { return emberTicksLeft; }
    public int charcoalWaiting() { return charcoalWaiting; }

    /** Light a cold fire (fire drill, strike-a-light or ember horn). */
    public boolean light() {
        if (state != State.COLD) return false;
        state = State.LIT;
        fuelTicks = LIGHTING_FUEL_TICKS;
        logsSinceLit = 0;
        return true;
    }

    /**
     * Add fuel. A cold fire must be lit first; fuel added to embers relights
     * the fire with no tool. Returns false when the fire cannot take it.
     */
    public boolean addFuel(Fuel fuel) {
        if (state == State.COLD) return false;
        if (state == State.LIT && fuelTicks >= kind.maxFuelTicks) return false;
        if (state == State.EMBERS) {
            state = State.LIT;
            fuelTicks = 0;
            emberTicksLeft = 0;
        }
        fuelTicks = Math.min(kind.maxFuelTicks, fuelTicks + fuel.ticks);
        if (fuel == Fuel.LOG) logsSinceLit++;
        return true;
    }

    /**
     * Advance by {@code ticks}. Large steps carry over between states, so a
     * long step can take a fire from lit through embers to cold. Fires only
     * tick while their chunk is loaded, like vanilla block entities.
     */
    public void tick(int ticks, Exposure exposure) {
        double remaining = ticks;
        while (remaining > 0) {
            switch (state) {
                case LIT -> {
                    double burn = kind.burnRate * exposure.burnMultiplier;
                    double ticksToEmpty = fuelTicks / burn;
                    if (remaining < ticksToEmpty) {
                        fuelTicks -= remaining * burn;
                        remaining = 0;
                    } else {
                        remaining -= ticksToEmpty;
                        fuelTicks = 0;
                        state = State.EMBERS;
                        emberTicksLeft = kind.emberTicks;
                    }
                }
                case EMBERS -> {
                    if (remaining < emberTicksLeft) {
                        emberTicksLeft -= (int) Math.round(remaining);
                        remaining = 0;
                    } else {
                        remaining -= emberTicksLeft;
                        emberTicksLeft = 0;
                        state = State.COLD;
                        charcoalWaiting = Math.min(MAX_CHARCOAL, charcoalWaiting + logsSinceLit / 2);
                        logsSinceLit = 0;
                    }
                }
                case COLD -> remaining = 0;
            }
        }
    }

    /** Collect charcoal from a cold fire with an empty hand. */
    public OptionalInt collectCharcoal() {
        if (state != State.COLD || charcoalWaiting == 0) return OptionalInt.empty();
        int n = charcoalWaiting;
        charcoalWaiting = 0;
        return OptionalInt.of(n);
    }

    /** Remaining burn time in real seconds at the current exposure, for the look-at readout. */
    public int secondsLeft(Exposure exposure) {
        if (state == State.EMBERS) return (int) Math.ceil(emberTicksLeft / 20.0);
        if (state == State.COLD) return 0;
        return (int) Math.ceil(fuelTicks / (kind.burnRate * exposure.burnMultiplier) / 20.0);
    }

    // --- persistence -----------------------------------------------------

    public record Snapshot(State state, double fuelTicks, int emberTicksLeft, int logsSinceLit,
                           int charcoalWaiting) {}

    public Snapshot save() {
        return new Snapshot(state, fuelTicks, emberTicksLeft, logsSinceLit, charcoalWaiting);
    }

    public void load(Snapshot s) {
        state = s.state();
        fuelTicks = s.fuelTicks();
        emberTicksLeft = s.emberTicksLeft();
        logsSinceLit = s.logsSinceLit();
        charcoalWaiting = s.charcoalWaiting();
    }
}
