package dev.paleolithic.rules.heat;

import dev.paleolithic.rules.Difficulty;
import dev.paleolithic.rules.heat.ClothingPiece.Material;
import dev.paleolithic.rules.heat.Surroundings.Weather;
import dev.paleolithic.rules.heat.Surroundings.Wetness;
import org.junit.Test;

import java.util.List;
import java.util.function.DoublePredicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The 13 reference scenarios from spec section 5. Acceptance criterion 2:
 * every scenario must hold within +-10%.
 */
public class HeatScenariosTest {

    private static final double REFUGE = 10;
    private static final double STEPPE = -8;
    private static final double SNOWY_PLAINS = -14;
    private static final double GLACIER = -22;
    private static final double CAMPFIRE_AT_2 = HeatSource.CAMPFIRE.contributionAt(2);

    private static final List<ClothingPiece> NONE = List.of();
    private static final List<ClothingPiece> HIDE = ClothingPiece.fullSet(Material.HIDE);
    private static final List<ClothingPiece> FUR = ClothingPiece.fullSet(Material.FUR);
    private static final List<ClothingPiece> LAYERED = ClothingPiece.fullSet(Material.LAYERED_FUR);

    private static double felt(Surroundings s) {
        return FeltTemperature.compute(s).total();
    }

    /** Seconds until the predicate holds, stepping once per second like the server. */
    private static int secondsUntil(double startB, Surroundings s, DoublePredicate reached) {
        BodyHeat heat = new BodyHeat();
        heat.load(new BodyHeat.Snapshot(startB, 0, 0, 0, 0, false));
        FeltTemperature.Breakdown b = FeltTemperature.compute(s);
        for (int second = 1; second <= 7200; second++) {
            heat.step(b.total(), b.environment(), Difficulty.NORMAL, 1.0, 20);
            if (reached.test(heat.value())) return second;
        }
        throw new AssertionError("threshold never reached");
    }

    private static void assertWithin10Percent(String what, int expectedSeconds, int actualSeconds) {
        double tolerance = expectedSeconds * 0.10;
        assertTrue(what + ": expected " + expectedSeconds + " s, got " + actualSeconds + " s",
                Math.abs(actualSeconds - expectedSeconds) <= tolerance);
    }

    private static final DoublePredicate CHILLY = b -> HeatState.of(b) == HeatState.CHILLY || b <= 60;
    private static final DoublePredicate COLD = b -> b <= 40;
    private static final DoublePredicate HYPOTHERMIC = b -> b <= 0;

    @Test
    public void s01_refugeDayNoClothing_doesNotCool() {
        Surroundings s = Surroundings.builder(REFUGE).clothing(NONE).build();
        assertEquals(10, felt(s), 1e-9);
        assertTrue(BodyHeat.ratePerSecond(felt(s), Difficulty.NORMAL, 1) >= 0);
    }

    @Test
    public void s02_refugeNightNoClothingNoFire() {
        Surroundings s = Surroundings.builder(REFUGE).night().clothing(NONE).build();
        assertEquals(4, felt(s), 1e-9);
        assertWithin10Percent("Chilly", 100, secondsUntil(75, s, CHILLY));
        assertWithin10Percent("Cold", 233, secondsUntil(75, s, COLD));
        assertWithin10Percent("Hypothermic", 500, secondsUntil(75, s, HYPOTHERMIC));
    }

    @Test
    public void s03_refugeNightCampfire() {
        Surroundings s = Surroundings.builder(REFUGE).night().heat(CAMPFIRE_AT_2).clothing(NONE).build();
        assertEquals(26, felt(s), 1e-9);
        assertEquals(0.65, BodyHeat.ratePerSecond(felt(s), Difficulty.NORMAL, 1), 1e-9);
    }

    @Test
    public void s04_steppeDayHide() {
        Surroundings s = Surroundings.builder(STEPPE).clothing(HIDE).build();
        assertEquals(2, felt(s), 1e-9);
        assertWithin10Percent("Cold", 300, secondsUntil(100, s, COLD));
        assertWithin10Percent("Hypothermic", 500, secondsUntil(100, s, HYPOTHERMIC));
    }

    @Test
    public void s05_steppeDayFur_warms() {
        Surroundings s = Surroundings.builder(STEPPE).clothing(FUR).build();
        assertEquals(14, felt(s), 1e-9);
        assertTrue(BodyHeat.ratePerSecond(felt(s), Difficulty.NORMAL, 1) > 0);
    }

    @Test
    public void s06_steppeNightFur() {
        Surroundings s = Surroundings.builder(STEPPE).night().clothing(FUR).build();
        assertEquals(8, felt(s), 1e-9);
        assertWithin10Percent("Cold", 1200, secondsUntil(100, s, COLD));
    }

    @Test
    public void s07_snowyPlainsDayBlizzardFur() {
        Surroundings s = Surroundings.builder(SNOWY_PLAINS).weather(Weather.THUNDER).clothing(FUR).build();
        assertEquals(0, felt(s), 1e-9);
        assertWithin10Percent("Cold", 240, secondsUntil(100, s, COLD));
    }

    @Test
    public void s08_snowyPlainsNightHideCampfireSheltered() {
        Surroundings s = Surroundings.builder(SNOWY_PLAINS).night().sheltered().heat(CAMPFIRE_AT_2)
                .clothing(HIDE).build();
        assertEquals(22, felt(s), 1e-9);
        assertEquals(0.55, BodyHeat.ratePerSecond(felt(s), Difficulty.NORMAL, 1), 1e-9);
    }

    @Test
    public void s09_glacierDayLayered() {
        Surroundings s = Surroundings.builder(GLACIER).clothing(LAYERED).build();
        assertEquals(8, felt(s), 1e-9);
        assertWithin10Percent("Cold", 1200, secondsUntil(100, s, COLD));
    }

    @Test
    public void s10_glacierNightBlizzardLayered() {
        Surroundings s = Surroundings.builder(GLACIER).night().weather(Weather.THUNDER).clothing(LAYERED).build();
        assertEquals(-6, felt(s), 1e-9);
        assertWithin10Percent("Cold", 150, secondsUntil(100, s, COLD));
        assertWithin10Percent("Hypothermic", 250, secondsUntil(100, s, HYPOTHERMIC));
    }

    @Test
    public void s11_glacierNightBlizzardNoClothing() {
        Surroundings s = Surroundings.builder(GLACIER).night().weather(Weather.THUNDER).clothing(NONE).build();
        assertEquals(-40, felt(s), 1e-9);
        assertWithin10Percent("Hypothermic", 60, secondsUntil(75, s, HYPOTHERMIC));
    }

    @Test
    public void s12_caveHide_warms() {
        Surroundings s = Surroundings.builder(GLACIER).underground().night().weather(Weather.THUNDER)
                .clothing(HIDE).build();
        assertEquals(20, felt(s), 1e-9);
        assertTrue(BodyHeat.ratePerSecond(felt(s), Difficulty.NORMAL, 1) > 0);
    }

    @Test
    public void s13_steppeDayFurSoaked() {
        Surroundings s = Surroundings.builder(STEPPE).wet(Wetness.WET).clothing(FUR).build();
        assertEquals(-9, felt(s), 1e-9);
        assertWithin10Percent("Cold", 126, secondsUntil(100, s, COLD));
    }
}
