package dev.paleolithic.rules.heat;

/**
 * A heat source: full strength out to {@code innerRadius}, falling linearly to
 * zero at {@code outerRadius} (spec section 5, "Heat sources").
 */
public record HeatSource(String id, double strength, double innerRadius, double outerRadius) {

    public HeatSource {
        if (outerRadius < innerRadius) {
            throw new IllegalArgumentException("outer radius below inner radius for " + id);
        }
    }

    /** Contribution at a given eye-to-block-centre distance, in degrees C. */
    public double contributionAt(double distance) {
        if (distance <= innerRadius) return strength;
        if (distance >= outerRadius) return 0;
        double t = (distance - innerRadius) / (outerRadius - innerRadius);
        return strength * (1 - t);
    }

    public static final HeatSource CAMPFIRE = new HeatSource("campfire", 22, 2, 7);
    public static final HeatSource HEARTH = new HeatSource("hearth", 26, 2, 9);
    public static final HeatSource EMBERS = new HeatSource("embers", 8, 1, 3);
    public static final HeatSource THERMAL_VENT = new HeatSource("thermal_vent", 18, 4, 10);
    public static final HeatSource LAVA = new HeatSource("lava", 30, 2, 6);
    public static final HeatSource FIRE = new HeatSource("fire", 10, 1, 4);
    public static final HeatSource STONE_LAMP = new HeatSource("stone_lamp", 5, 3, 5);
    public static final HeatSource MAGMA_BLOCK = new HeatSource("magma_block", 6, 1, 2);
    public static final HeatSource TORCH = new HeatSource("torch", 3, 2, 2);
}
