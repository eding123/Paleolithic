package dev.paleolithic.rules.heat;

/**
 * Tracks wetness: wet while in water and for 60 seconds after leaving water
 * or rain (spec section 5). Thermal spring water never makes a player wet.
 */
public final class WetTracker {

    public static final int DRY_AFTER_TICKS = 60 * 20;

    private int ticksSinceWet = Integer.MAX_VALUE / 2;

    /**
     * @param inWater        player is touching water
     * @param submerged      player's eyes are under water
     * @param inRain         player is exposed to falling rain (not snow)
     * @param inThermalWater the water is part of a hot spring
     * @param ticks          ticks elapsed since the last call
     */
    public Surroundings.Wetness update(boolean inWater, boolean submerged, boolean inRain,
                                       boolean inThermalWater, int ticks) {
        if (inThermalWater) {
            ticksSinceWet = Integer.MAX_VALUE / 2;
            return Surroundings.Wetness.DRY;
        }
        if (submerged) {
            ticksSinceWet = 0;
            return Surroundings.Wetness.SUBMERGED;
        }
        if (inWater || inRain) {
            ticksSinceWet = 0;
            return Surroundings.Wetness.WET;
        }
        ticksSinceWet += ticks;
        return ticksSinceWet < DRY_AFTER_TICKS ? Surroundings.Wetness.WET : Surroundings.Wetness.DRY;
    }

    public int ticksSinceWet() {
        return ticksSinceWet;
    }

    public void load(int ticksSinceWet) {
        this.ticksSinceWet = ticksSinceWet;
    }
}
