package dev.paleolithic.rules.hunting;

import dev.paleolithic.rules.hunting.CarcassYield.Tool;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

public class HuntingTest {

    // --- detection -----------------------------------------------------------

    @Test
    public void downwindMeansTheWindBlowsFromTheAnimalToThePlayer() {
        // Wind blowing toward the east; player east of the animal.
        assertEquals(Detection.WindRelation.DOWNWIND,
                Detection.windRelation(Wind.Direction.E, 0, 0, 10, 0));
        assertEquals(Detection.WindRelation.UPWIND,
                Detection.windRelation(Wind.Direction.E, 0, 0, -10, 0));
        assertEquals(Detection.WindRelation.CROSSWIND,
                Detection.windRelation(Wind.Direction.E, 0, 0, 0, 10));
        // Exactly 45 degrees off still counts as downwind.
        assertEquals(Detection.WindRelation.DOWNWIND,
                Detection.windRelation(Wind.Direction.E, 0, 0, 10, 10));
    }

    @Test
    public void detectionMultipliersStack() {
        double r = Detection.effectiveRadius(20, true, Detection.WindRelation.DOWNWIND, true, false, false);
        assertEquals(20 * 0.5 * 0.6 * 0.75, r, 1e-9);
        assertEquals(30, Detection.effectiveRadius(20, false, Detection.WindRelation.UPWIND, false, false, false),
                1e-9);
        assertEquals(20 * 0.5 * 0.8,
                Detection.effectiveRadius(20, false, Detection.WindRelation.CROSSWIND, false, true, true), 1e-9);
    }

    @Test
    public void windShiftsAtMost90DegreesOnSchedule() {
        Random random = new Random(42);
        Wind wind = new Wind(Wind.Direction.N, 0);
        long time = 0;
        for (int i = 0; i < 500; i++) {
            Wind.Direction before = wind.direction();
            wind.update(time, random);
            int diff = Math.floorMod(wind.direction().ordinal() - before.ordinal(), 8);
            assertTrue(diff <= 2 || diff >= 6);
            long gap = wind.nextChangeTick() - time;
            assertTrue(gap >= Wind.MIN_CHANGE_TICKS && gap <= Wind.MAX_CHANGE_TICKS);
            assertFalse(wind.update(time + 1, random));
            time = wind.nextChangeTick();
        }
    }

    // --- game pressure -------------------------------------------------------

    @Test
    public void pressureLowersSpawnsWithAFloorAndRecoversDaily() {
        GamePressure p = new GamePressure();
        GamePressure.RegionKey region = GamePressure.RegionKey.ofBlock(100, -100);
        assertEquals(new GamePressure.RegionKey(0, -1), region);

        assertTrue(p.recordKill(region, "woolly_mammoth", 4, 0).isEmpty());
        assertEquals(GamePressure.Message.WARY, p.recordKill(region, "woolly_mammoth", 4, 0).orElseThrow());
        assertFalse(p.calvesAllowed(region, "woolly_mammoth", 4, 0));
        p.recordKill(region, "woolly_mammoth", 4, 0);
        assertEquals(GamePressure.Message.SCARCE, p.recordKill(region, "woolly_mammoth", 4, 0).orElseThrow());
        assertEquals(0.2, p.spawnMultiplier(region, "woolly_mammoth", 4, 0), 1e-9);

        long twoDays = 2 * GamePressure.TICKS_PER_DAY;
        assertEquals(2, p.kills(region, "woolly_mammoth", twoDays), 1e-9);
        assertEquals(0.5, p.spawnMultiplier(region, "woolly_mammoth", 4, twoDays), 1e-9);
        assertEquals(1.0, p.spawnMultiplier(region, "steppe_bison", 12, twoDays), 1e-9);
    }

    @Test
    public void theThawRaisesCaps() {
        GamePressure p = new GamePressure();
        p.setThaw(true);
        GamePressure.RegionKey region = new GamePressure.RegionKey(0, 0);
        for (int i = 0; i < 4; i++) p.recordKill(region, "woolly_mammoth", 4, 0);
        assertEquals(1 - 4 / 6.0, p.spawnMultiplier(region, "woolly_mammoth", 4, 0), 1e-9);
    }

    @Test
    public void pressureSurvivesSaveAndLoad() {
        GamePressure p = new GamePressure();
        GamePressure.RegionKey region = new GamePressure.RegionKey(3, 4);
        p.recordKill(region, "reindeer", 16, 100);
        GamePressure copy = new GamePressure();
        copy.load(p.save());
        assertEquals(1, copy.kills(region, "reindeer", 100), 1e-9);
    }

    // --- carcasses -----------------------------------------------------------

    private static int totalItems(List<CarcassYield.Cut> plan) {
        return plan.stream().mapToInt(c -> c.items().values().stream().mapToInt(Integer::intValue).sum()).sum();
    }

    @Test
    public void mammothSplitsIntoTwelveEvenCutsHideFirst() {
        CarcassYield mammoth = CarcassYields.DEFAULTS.get("woolly_mammoth");
        List<CarcassYield.Cut> plan = mammoth.plan(Tool.KNIFE, false, false);
        assertEquals(12, plan.size());
        assertEquals(29, totalItems(plan));
        for (CarcassYield.Cut cut : plan) {
            int n = cut.items().values().stream().mapToInt(Integer::intValue).sum();
            assertTrue(n == 2 || n == 3);
        }
        assertTrue(plan.get(0).items().containsKey("paleolithic:raw_pelt"));
        assertTrue(plan.get(11).items().containsKey("paleolithic:ivory_tusk"));
    }

    @Test
    public void handAxeGives75PercentRoundedDownWithAMinimumOfOne() {
        CarcassYield bison = CarcassYields.DEFAULTS.get("steppe_bison");
        List<CarcassYield.Stack> t = bison.totals(Tool.HAND_AXE, false, false);
        assertEquals(1, t.get(0).count()); // 2 hides -> 1.5 -> 1
        assertEquals(3, t.get(1).count()); // 5 meat -> 3.75 -> 3
        assertEquals(1, t.get(2).count()); // 1 fat -> 0.75 -> min 1
    }

    @Test
    public void primeCarcassAddsHalfMeatAndFatAndDoublesHides() {
        CarcassYield bison = CarcassYields.DEFAULTS.get("steppe_bison");
        List<CarcassYield.Stack> t = bison.totals(Tool.KNIFE, true, false);
        assertEquals(4, t.get(0).count()); // hides doubled
        assertEquals(8, t.get(1).count()); // 5 * 1.5 = 7.5 -> 8
        assertEquals(2, t.get(2).count()); // 1 * 1.5 = 1.5 -> 2
        assertEquals(2, t.get(3).count()); // sinew unchanged
    }

    @Test
    public void legendsDoubleYieldAndCuts() {
        CarcassYield bear = CarcassYields.DEFAULTS.get("cave_bear");
        List<CarcassYield.Cut> plan = bear.plan(Tool.KNIFE, false, true);
        assertEquals(14, plan.size());
        assertEquals(30, totalItems(plan));
    }

    @Test
    public void practisedKnifeCutsFaster() {
        assertEquals(20, CarcassYield.ticksPerCut(Tool.KNIFE, 9));
        assertEquals(15, CarcassYield.ticksPerCut(Tool.KNIFE, 10));
        assertEquals(30, CarcassYield.ticksPerCut(Tool.HAND_AXE, 50));
    }
}
