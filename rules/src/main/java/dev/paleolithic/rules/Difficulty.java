package dev.paleolithic.rules;

/**
 * Vanilla difficulty as it affects Paleolithic's cold (spec section 18).
 */
public enum Difficulty {
    PEACEFUL(0.5, 0, 41),
    EASY(0.75, 80, 0),
    NORMAL(1.0, 40, 0),
    HARD(1.25, 30, 0);

    /** Multiplier on body-heat loss. The 1.5/s cap applies after it. */
    public final double coldLossMultiplier;
    /** Ticks between hypothermia damage points; 0 = no damage. */
    public final int hypothermiaDamageIntervalTicks;
    /** Body heat never falls below this value. */
    public final double bodyHeatFloor;

    Difficulty(double coldLossMultiplier, int hypothermiaDamageIntervalTicks, double bodyHeatFloor) {
        this.coldLossMultiplier = coldLossMultiplier;
        this.hypothermiaDamageIntervalTicks = hypothermiaDamageIntervalTicks;
        this.bodyHeatFloor = bodyHeatFloor;
    }
}
