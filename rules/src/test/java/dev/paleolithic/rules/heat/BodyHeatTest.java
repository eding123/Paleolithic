package dev.paleolithic.rules.heat;

import dev.paleolithic.rules.Difficulty;
import dev.paleolithic.rules.heat.ClothingPiece.Material;
import dev.paleolithic.rules.heat.ClothingPiece.Slot;
import dev.paleolithic.rules.heat.Surroundings.Weather;
import dev.paleolithic.rules.heat.Surroundings.Wetness;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BodyHeatTest {

    private static BodyHeat at(double b) {
        BodyHeat h = new BodyHeat();
        h.load(new BodyHeat.Snapshot(b, 0, 0, 0, 0, false));
        return h;
    }

    @Test
    public void lossIsCappedAfterEveryMultiplier_soTheWarningIsAtLeast40Seconds() {
        for (Difficulty d : new Difficulty[]{Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD}) {
            double rate = BodyHeat.ratePerSecond(-200, d, 3.0);
            assertEquals(d.name(), -1.5, rate, 1e-9);
            BodyHeat h = at(60);
            int seconds = 0;
            while (h.value() > 0) {
                h.step(-200, -200, d, 3.0, 20);
                seconds++;
            }
            assertTrue(d + " warning was " + seconds + " s", seconds >= 40);
        }
    }

    @Test
    public void gainIsCappedAt2() {
        assertEquals(2.0, BodyHeat.ratePerSecond(500, Difficulty.NORMAL, 1), 1e-9);
    }

    @Test
    public void peacefulNeverFallsBelow41AndDealsNoDamage() {
        BodyHeat h = at(60);
        int damage = 0;
        for (int i = 0; i < 600; i++) damage += h.step(-60, -60, Difficulty.PEACEFUL, 1, 20).damagePoints();
        assertEquals(41, h.value(), 1e-9);
        assertEquals(0, damage);
    }

    @Test
    public void normalHypothermiaDeals1PointEvery2Seconds() {
        BodyHeat h = at(0);
        int damage = 0;
        for (int i = 0; i < 10; i++) damage += h.step(-60, -60, Difficulty.NORMAL, 1, 20).damagePoints();
        assertEquals(5, damage);
    }

    @Test
    public void hardHypothermiaDeals1PointEvery1point5Seconds() {
        BodyHeat h = at(0);
        int damage = 0;
        for (int i = 0; i < 6; i++) damage += h.step(-60, -60, Difficulty.HARD, 1, 20).damagePoints();
        assertEquals(4, damage);
    }

    @Test
    public void secondWindStopsLossFor30Seconds() {
        BodyHeat h = new BodyHeat();
        h.respawn();
        for (int i = 0; i < 30; i++) h.step(-60, -60, Difficulty.NORMAL, 1, 20);
        assertEquals(75, h.value(), 1e-9);
        h.step(-60, -60, Difficulty.NORMAL, 1, 20);
        assertTrue(h.value() < 75);
    }

    @Test
    public void heatStressStartsAfter30SecondsAndClears10SecondsAfterCooling() {
        BodyHeat h = at(100);
        for (int i = 0; i < 29; i++) assertFalse(h.step(40, 24, Difficulty.NORMAL, 1, 20).overheated());
        assertTrue(h.step(40, 24, Difficulty.NORMAL, 1, 20).overheated());
        for (int i = 0; i < 9; i++) assertTrue(h.step(25, 24, Difficulty.NORMAL, 1, 20).overheated());
        assertFalse(h.step(25, 24, Difficulty.NORMAL, 1, 20).overheated());
    }

    @Test
    public void clothingNeverCausesHeatStressInTheCold() {
        BodyHeat h = at(100);
        for (int i = 0; i < 120; i++) assertFalse(h.step(40, 6, Difficulty.NORMAL, 1, 20).overheated());
    }

    @Test
    public void trendReportsFastCooling() {
        assertEquals(BodyHeat.Trend.COOLING_FAST, at(80).step(-20, -20, Difficulty.NORMAL, 1, 20).trend());
        assertEquals(BodyHeat.Trend.COOLING, at(80).step(5, 5, Difficulty.NORMAL, 1, 20).trend());
        assertEquals(BodyHeat.Trend.WARMING, at(80).step(20, 20, Difficulty.NORMAL, 1, 20).trend());
        assertEquals(BodyHeat.Trend.STABLE, at(100).step(20, 20, Difficulty.NORMAL, 1, 20).trend());
    }

    @Test
    public void statesMatchThresholds() {
        assertEquals(HeatState.WARM, HeatState.of(61));
        assertEquals(HeatState.CHILLY, HeatState.of(60));
        assertEquals(HeatState.COLD, HeatState.of(40));
        assertEquals(HeatState.FREEZING, HeatState.of(20));
        assertEquals(HeatState.HYPOTHERMIC, HeatState.of(0));
    }

    // --- felt temperature details ------------------------------------------

    @Test
    public void shelterCancelsWindAndPrecipitation() {
        Surroundings open = Surroundings.builder(-14).weather(Weather.PRECIPITATION).build();
        Surroundings hut = Surroundings.builder(-14).weather(Weather.PRECIPITATION).sheltered().build();
        assertEquals(-14 - 3 - 4, FeltTemperature.compute(open).total(), 1e-9);
        assertEquals(-14 + 6, FeltTemperature.compute(hut).total(), 1e-9);
        Surroundings insulatedHut = Surroundings.builder(-14).weather(Weather.PRECIPITATION).insulated().build();
        assertEquals(-14 + 10, FeltTemperature.compute(insulatedHut).total(), 1e-9);
    }

    @Test
    public void thunderInAWarmBiomeIsOrdinaryRain() {
        Surroundings s = Surroundings.builder(24).weather(Weather.THUNDER).build();
        assertEquals(21, FeltTemperature.compute(s).total(), 1e-9);
        assertFalse(FeltTemperature.isBlizzard(s));
    }

    @Test
    public void noWindBelowY63OrInWarmBiomes() {
        assertEquals(-14, FeltTemperature.compute(Surroundings.builder(-14).headY(62).build()).total(), 1e-9);
        assertEquals(10, FeltTemperature.compute(Surroundings.builder(10).headY(90).build()).total(), 1e-9);
    }

    @Test
    public void altitudeCostsOneDegreePer8BlocksAbove100() {
        assertEquals(10 - 2, FeltTemperature.compute(Surroundings.builder(10).headY(116).build()).total(), 1e-9);
        assertEquals(10 - 1, FeltTemperature.compute(Surroundings.builder(10).headY(115).build()).total(), 1e-9);
        assertEquals(10, FeltTemperature.compute(Surroundings.builder(10).headY(100).build()).total(), 1e-9);
    }

    @Test
    public void wetnessHalvesHideAndFurButOnlyQuartersLayeredFur() {
        List<ClothingPiece> mixed = List.of(ClothingPiece.of(Material.HIDE, Slot.TUNIC),
                ClothingPiece.of(Material.LAYERED_FUR, Slot.HOOD));
        Surroundings wet = Surroundings.builder(10).wet(Wetness.WET).clothing(mixed).build();
        assertEquals(5 * 0.5 + 6 * 0.75, FeltTemperature.compute(wet).clothing(), 1e-9);
        Surroundings submerged = Surroundings.builder(10).wet(Wetness.SUBMERGED).build();
        assertEquals(-5, FeltTemperature.compute(submerged).total(), 1e-9);
    }

    @Test
    public void fullSetTotalsMatchTheSpec() {
        assertEquals(14, sum(ClothingPiece.fullSet(Material.HIDE)), 1e-9);
        assertEquals(26, sum(ClothingPiece.fullSet(Material.FUR)), 1e-9);
        assertEquals(34, sum(ClothingPiece.fullSet(Material.LAYERED_FUR)), 1e-9);
    }

    private static double sum(List<ClothingPiece> pieces) {
        return pieces.stream().mapToDouble(ClothingPiece::insulation).sum();
    }

    @Test
    public void heatSourceFallsOffLinearly() {
        assertEquals(22, HeatSource.CAMPFIRE.contributionAt(1.5), 1e-9);
        assertEquals(11, HeatSource.CAMPFIRE.contributionAt(4.5), 1e-9);
        assertEquals(0, HeatSource.CAMPFIRE.contributionAt(7), 1e-9);
        assertEquals(3, HeatSource.TORCH.contributionAt(2), 1e-9);
        assertEquals(0, HeatSource.TORCH.contributionAt(2.01), 1e-9);
    }

    @Test
    public void unknownBiomesUseTheFallbackFormula() {
        BiomeClimate climate = BiomeClimate.defaults();
        assertEquals(-8, climate.baseTemperature("paleolithic:mammoth_steppe", 0.5), 1e-9);
        assertEquals(-14, climate.baseTemperature("othermod:frost", 0.0), 1e-9);
        assertEquals(28, climate.baseTemperature("othermod:volcano", 2.0), 1e-9);
        assertEquals(-25, climate.baseTemperature("othermod:void", -1.0), 1e-9);
    }

    @Test
    public void wetTrackerStaysWet60SecondsAndIgnoresThermalWater() {
        WetTracker w = new WetTracker();
        assertEquals(Wetness.WET, w.update(true, false, false, false, 20));
        for (int s = 0; s < 59; s++) assertEquals(Wetness.WET, w.update(false, false, false, false, 20));
        assertEquals(Wetness.DRY, w.update(false, false, false, false, 20));
        assertEquals(Wetness.DRY, w.update(true, true, false, true, 20));
    }
}
