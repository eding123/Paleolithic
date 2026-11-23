package dev.paleolithic.rules.heat;

/**
 * Body-heat states and their thresholds (spec section 5, "States and effects").
 */
public enum HeatState {
    WARM, CHILLY, COLD, FREEZING, HYPOTHERMIC;

    public static HeatState of(double bodyHeat) {
        if (bodyHeat <= 0) return HYPOTHERMIC;
        if (bodyHeat <= 20) return FREEZING;
        if (bodyHeat <= 40) return COLD;
        if (bodyHeat <= 60) return CHILLY;
        return WARM;
    }

    /** Multiplier applied to natural health regeneration. */
    public double regenerationMultiplier() {
        return switch (this) {
            case WARM -> 1.0;
            case CHILLY -> 0.5;
            default -> 0.0;
        };
    }

    /** Extra hunger exhaustion: 0.25 means +25%. */
    public double extraExhaustion() {
        return switch (this) {
            case COLD -> 0.25;
            case FREEZING, HYPOTHERMIC -> 0.5;
            default -> 0.0;
        };
    }

    /** Slowness amplifier (0 = Slowness I), or -1 for none. */
    public int slownessAmplifier() {
        return switch (this) {
            case COLD -> 0;
            case FREEZING, HYPOTHERMIC -> 1;
            default -> -1;
        };
    }

    /** Mining Fatigue amplifier, or -1 for none. */
    public int miningFatigueAmplifier() {
        return (this == FREEZING || this == HYPOTHERMIC) ? 0 : -1;
    }
}
