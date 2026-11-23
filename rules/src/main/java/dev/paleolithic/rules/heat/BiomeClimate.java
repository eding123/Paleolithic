package dev.paleolithic.rules.heat;

import java.util.HashMap;
import java.util.Map;

/**
 * Biome base temperatures (spec section 5, "Biome base temperatures (day)").
 * The core module can override any entry from data; these are the defaults.
 */
public final class BiomeClimate {

    private final Map<String, Double> baseByBiome;

    public BiomeClimate(Map<String, Double> baseByBiome) {
        this.baseByBiome = Map.copyOf(baseByBiome);
    }

    /**
     * Base temperature for a biome id. Unknown biomes (for example from
     * another mod) use clamp(30 t - 14, -25, 28), where t is the biome's
     * vanilla temperature value.
     */
    public double baseTemperature(String biomeId, double vanillaTemperature) {
        Double known = baseByBiome.get(biomeId);
        if (known != null) return known;
        return fallback(vanillaTemperature);
    }

    public static double fallback(double vanillaTemperature) {
        return Math.max(-25, Math.min(28, 30 * vanillaTemperature - 14));
    }

    /** Wind applies only where the base temperature is 0 or below. */
    public static boolean windExposed(double baseTemperature) {
        return baseTemperature <= 0;
    }

    public static BiomeClimate defaults() {
        Map<String, Double> m = new HashMap<>();
        put(m, -22, "paleolithic:glacier");
        put(m, -20, "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:ice_spikes");
        put(m, -16, "minecraft:snowy_slopes", "minecraft:frozen_ocean", "minecraft:deep_frozen_ocean",
                "minecraft:frozen_river");
        put(m, -14, "minecraft:snowy_plains", "minecraft:snowy_beach", "paleolithic:tundra");
        put(m, -10, "minecraft:snowy_taiga", "minecraft:grove");
        put(m, -8, "paleolithic:mammoth_steppe");
        put(m, -2, "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                "minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills",
                "minecraft:stony_peaks", "minecraft:stony_shore", "minecraft:cold_ocean",
                "minecraft:deep_cold_ocean");
        put(m, 10, "minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest",
                "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest",
                "minecraft:flower_forest", "minecraft:cherry_grove", "minecraft:meadow", "minecraft:river",
                "minecraft:beach", "minecraft:ocean", "minecraft:deep_ocean", "minecraft:pale_garden");
        put(m, 24, "minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna",
                "minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle", "minecraft:desert",
                "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands",
                "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:warm_ocean",
                "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean", "minecraft:mushroom_fields");
        return new BiomeClimate(m);
    }

    private static void put(Map<String, Double> m, double value, String... ids) {
        for (String id : ids) m.put(id, value);
    }
}
