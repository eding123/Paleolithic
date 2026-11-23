package dev.paleolithic.rules.hunting;

/**
 * Effective detection radius for a creature noticing a player (spec section 12).
 */
public final class Detection {

    public static final double SNEAKING = 0.5;
    public static final double DOWNWIND = 0.6;
    public static final double UPWIND = 1.5;
    public static final double NIGHT = 0.75;
    public static final double BLIZZARD = 0.5;
    public static final double NIGHT_EYES = 0.8;
    /** cos(45 degrees): the half-angle of the downwind and upwind cones. */
    private static final double CONE = Math.cos(Math.toRadians(45)) - 1e-9;

    private Detection() {}

    public enum WindRelation { DOWNWIND, UPWIND, CROSSWIND, SAME_SPOT }

    /**
     * The player is downwind when the wind blows from the animal toward the
     * player (within 45 degrees), so the player's scent is carried away.
     */
    public static WindRelation windRelation(Wind.Direction wind, double animalX, double animalZ,
                                            double playerX, double playerZ) {
        double dx = playerX - animalX;
        double dz = playerZ - animalZ;
        double len = Math.hypot(dx, dz);
        if (len < 1e-6) return WindRelation.SAME_SPOT;
        double cos = (dx * wind.dx + dz * wind.dz) / len;
        if (cos >= CONE) return WindRelation.DOWNWIND;
        if (cos <= -CONE) return WindRelation.UPWIND;
        return WindRelation.CROSSWIND;
    }

    public static double effectiveRadius(double baseRadius, boolean sneaking, WindRelation wind, boolean night,
                                         boolean blizzard, boolean nightEyes) {
        double r = baseRadius;
        if (sneaking) r *= SNEAKING;
        if (wind == WindRelation.DOWNWIND) r *= DOWNWIND;
        if (wind == WindRelation.UPWIND) r *= UPWIND;
        if (night) r *= NIGHT;
        if (blizzard) r *= BLIZZARD;
        if (nightEyes) r *= NIGHT_EYES;
        return r;
    }
}
