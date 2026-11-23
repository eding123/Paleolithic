package dev.paleolithic.rules.heat;

/**
 * One worn clothing piece and its insulation in degrees C (spec section 11).
 */
public record ClothingPiece(Material material, Slot slot, double insulation) {

    public enum Material {
        HIDE(0.5), FUR(0.5), LAYERED_FUR(0.75), OTHER(1.0);

        /** Fraction of insulation kept while the wearer is wet (spec section 5). */
        public final double wetFactor;

        Material(double wetFactor) {
            this.wetFactor = wetFactor;
        }
    }

    public enum Slot { HOOD, TUNIC, LEGGINGS, BOOTS }

    public double effectiveInsulation(boolean wet) {
        return wet ? insulation * material.wetFactor : insulation;
    }

    public static ClothingPiece of(Material material, Slot slot) {
        return new ClothingPiece(material, slot, defaultInsulation(material, slot));
    }

    /** Defaults from the clothing table in spec section 11. */
    public static double defaultInsulation(Material material, Slot slot) {
        return switch (material) {
            case HIDE -> switch (slot) { case HOOD -> 3; case TUNIC -> 5; case LEGGINGS -> 4; case BOOTS -> 2; };
            case FUR -> switch (slot) { case HOOD -> 5; case TUNIC -> 10; case LEGGINGS -> 7; case BOOTS -> 4; };
            case LAYERED_FUR -> switch (slot) { case HOOD -> 6; case TUNIC -> 13; case LEGGINGS -> 9; case BOOTS -> 6; };
            case OTHER -> 0;
        };
    }

    public static java.util.List<ClothingPiece> fullSet(Material material) {
        return java.util.Arrays.stream(Slot.values()).map(s -> of(material, s)).toList();
    }
}
