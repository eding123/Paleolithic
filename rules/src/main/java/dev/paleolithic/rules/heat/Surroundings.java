package dev.paleolithic.rules.heat;

import java.util.List;

/**
 * Everything the felt-temperature formula needs about one player at one
 * moment. The platform layer samples the world and fills this in once per
 * second; the rules never touch the world themselves.
 *
 * @param biomeBase        base temperature of the biome at the player's feet
 * @param underground      true when the sky light level at the player's head is 0
 * @param night            true from time 12,542 to 23,460
 * @param weather          weather at the player's position
 * @param exposedToSky     true when nothing blocks the sky above the player's head
 * @param sheltered        result of the shelter test
 * @param insulated        result of the shelter test (implies sheltered)
 * @param headY            block Y of the player's head
 * @param wet              wetness from {@link WetTracker}
 * @param heatContribution strongest single heat source in range, in degrees C
 * @param clothing         worn clothing pieces
 * @param bonus            blessing and Thaw bonuses combined, in degrees C
 */
public record Surroundings(
        double biomeBase,
        boolean underground,
        boolean night,
        Weather weather,
        boolean exposedToSky,
        boolean sheltered,
        boolean insulated,
        int headY,
        Wetness wet,
        double heatContribution,
        List<ClothingPiece> clothing,
        double bonus) {

    public enum Weather { CLEAR, PRECIPITATION, THUNDER }

    public enum Wetness { DRY, WET, SUBMERGED }

    public Surroundings {
        clothing = List.copyOf(clothing);
        if (insulated && !sheltered) {
            throw new IllegalArgumentException("insulated implies sheltered");
        }
    }

    /** Convenience builder for tests and the platform layer. */
    public static Builder builder(double biomeBase) {
        return new Builder(biomeBase);
    }

    public static final class Builder {
        private final double biomeBase;
        private boolean underground;
        private boolean night;
        private Weather weather = Weather.CLEAR;
        private boolean exposedToSky = true;
        private boolean sheltered;
        private boolean insulated;
        private int headY = 70;
        private Wetness wet = Wetness.DRY;
        private double heat;
        private List<ClothingPiece> clothing = List.of();
        private double bonus;

        private Builder(double biomeBase) { this.biomeBase = biomeBase; }

        public Builder underground() { underground = true; exposedToSky = false; return this; }
        public Builder night() { night = true; return this; }
        public Builder weather(Weather w) { weather = w; return this; }
        public Builder covered() { exposedToSky = false; return this; }
        public Builder sheltered() { sheltered = true; exposedToSky = false; return this; }
        public Builder insulated() { sheltered(); insulated = true; return this; }
        public Builder headY(int y) { headY = y; return this; }
        public Builder wet(Wetness w) { wet = w; return this; }
        public Builder heat(double contribution) { heat = contribution; return this; }
        public Builder clothing(List<ClothingPiece> c) { clothing = c; return this; }
        public Builder bonus(double b) { bonus = b; return this; }

        public Surroundings build() {
            return new Surroundings(biomeBase, underground, night, weather, exposedToSky, sheltered,
                    insulated, headY, wet, heat, clothing, bonus);
        }
    }
}
