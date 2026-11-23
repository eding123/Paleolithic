package dev.paleolithic.rules.heat;

/**
 * The felt-temperature formula (spec section 5):
 * T = T_env + T_shelter + T_heat + I_clothing + T_blessing + T_thaw.
 */
public final class FeltTemperature {

    public static final double UNDERGROUND_TEMPERATURE = 6;
    public static final double NIGHT = -6;
    public static final double PRECIPITATION = -3;
    public static final double BLIZZARD = -8;
    public static final double WIND = -4;
    public static final int WIND_MIN_Y = 63;
    public static final int ALTITUDE_START_Y = 100;
    public static final int ALTITUDE_BLOCKS_PER_DEGREE = 8;
    public static final double WET = -10;
    public static final double SUBMERGED = -15;
    public static final double SHELTERED = 6;
    public static final double INSULATED = 10;

    private FeltTemperature() {}

    /** Every term of the formula, kept separate for debug output and the numeric HUD. */
    public record Breakdown(double environment, double shelter, double heat, double clothing, double bonus) {
        public double total() {
            return environment + shelter + heat + clothing + bonus;
        }
    }

    public static Breakdown compute(Surroundings s) {
        double env;
        double shelter;
        if (s.underground()) {
            // Caves are a flat +6 C. Surface modifiers and the shelter bonus do
            // not apply: the flat value already accounts for the enclosure
            // (this matches reference scenario 12, "any cave, hide set: 20 C").
            env = UNDERGROUND_TEMPERATURE;
            shelter = 0;
        } else {
            env = surfaceEnvironment(s);
            shelter = s.insulated() ? INSULATED : s.sheltered() ? SHELTERED : 0;
        }
        boolean wet = s.wet() != Surroundings.Wetness.DRY;
        double clothing = 0;
        for (ClothingPiece piece : s.clothing()) clothing += piece.effectiveInsulation(wet);
        return new Breakdown(env, shelter, s.heatContribution(), clothing, s.bonus());
    }

    static double surfaceEnvironment(Surroundings s) {
        double t = s.biomeBase();
        if (s.night()) t += NIGHT;

        boolean coldBiome = BiomeClimate.windExposed(s.biomeBase());
        if (!s.sheltered() && s.exposedToSky()) {
            switch (s.weather()) {
                case PRECIPITATION -> t += PRECIPITATION;
                case THUNDER -> t += coldBiome ? BLIZZARD : PRECIPITATION;
                case CLEAR -> { }
            }
        }
        if (coldBiome && !s.sheltered() && s.headY() >= WIND_MIN_Y) t += WIND;

        if (s.headY() > ALTITUDE_START_Y) {
            t -= Math.floorDiv(s.headY() - ALTITUDE_START_Y, ALTITUDE_BLOCKS_PER_DEGREE);
        }

        t += switch (s.wet()) {
            case DRY -> 0;
            case WET -> WET;
            case SUBMERGED -> SUBMERGED;
        };
        return t;
    }

    /** True when the player is in a blizzard: thunder weather in a cold biome. */
    public static boolean isBlizzard(Surroundings s) {
        return s.weather() == Surroundings.Weather.THUNDER && BiomeClimate.windExposed(s.biomeBase());
    }
}
