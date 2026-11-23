package dev.paleolithic.rules.knapping;

import dev.paleolithic.rules.knapping.KnappingPattern.Material;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class KnappingTest {

    @Test
    public void wasteCellCountsMatchThePatternGrids() {
        assertEquals(10, KnappingPattern.HAND_AXE.wasteCells());
        assertEquals(17, KnappingPattern.KNIFE_BLADE.wasteCells());
        assertEquals(12, KnappingPattern.SCRAPER.wasteCells());
        assertEquals(14, KnappingPattern.SPEAR_POINT.wasteCells());
        assertEquals(6, KnappingPattern.AXE_HEAD.wasteCells());
        assertEquals(16, KnappingPattern.PICK_HEAD.wasteCells());
        assertEquals(15, KnappingPattern.SPADE_HEAD.wasteCells());
        assertEquals(19, KnappingPattern.DART_POINT.wasteCells());
    }

    @Test
    public void materialsOfferTheRightPatterns() {
        assertEquals(8, KnappingPattern.forMaterial(Material.FLINT).size());
        assertEquals(6, KnappingPattern.forMaterial(Material.HEAT_TREATED_FLINT).size());
        assertFalse(KnappingPattern.forMaterial(Material.HEAT_TREATED_FLINT).contains(KnappingPattern.HAND_AXE));
        assertEquals(3, KnappingPattern.forMaterial(Material.OBSIDIAN).size());
    }

    @Test
    public void headCellsAreLockedSoAPieceCanNeverBeRuined() {
        KnappingSession s = new KnappingSession(Material.FLINT);
        assertEquals(KnappingSession.ClickResult.NO_PATTERN, s.click(0));
        assertTrue(s.choose(KnappingPattern.HAND_AXE));
        assertEquals(KnappingSession.ClickResult.LOCKED, s.click(2));
        assertEquals(KnappingSession.ClickResult.REMOVED, s.click(0));
        assertEquals(KnappingSession.ClickResult.ALREADY_REMOVED, s.click(0));
        assertEquals(KnappingSession.ClickResult.OUT_OF_RANGE, s.click(25));
    }

    @Test
    public void removingAllWasteFinishesWithTheRightOutput() {
        KnappingSession s = new KnappingSession(Material.OBSIDIAN);
        assertTrue(s.choose(KnappingPattern.DART_POINT));
        int clicks = 0;
        for (int i = 0; i < 25; i++) {
            if (s.click(i) == KnappingSession.ClickResult.REMOVED) clicks++;
        }
        assertEquals(19, clicks);
        assertTrue(s.finished());
        KnappingSession.Output out = s.output().orElseThrow();
        assertEquals("paleolithic:obsidian_dart_point", out.itemId());
        assertEquals(2, out.count());
    }

    @Test
    public void obsidianCannotMakeAHandAxe() {
        assertFalse(new KnappingSession(Material.OBSIDIAN).choose(KnappingPattern.HAND_AXE));
    }

    @Test
    public void patternCanChangeOnlyIfNoNeededCellWasChipped() {
        KnappingSession s = new KnappingSession(Material.FLINT);
        s.choose(KnappingPattern.SCRAPER);
        s.click(0); // corner: waste for both scraper and axe head? No: axe head keeps (0,0).
        assertFalse(s.choose(KnappingPattern.AXE_HEAD));
        assertTrue(s.choose(KnappingPattern.SPEAR_POINT));
    }

    @Test
    public void strikeNeedsFivePracticedCompletionsUnlessTheMinigameIsOff() {
        KnappingSession s = new KnappingSession(Material.FLINT);
        s.choose(KnappingPattern.SPEAR_POINT);
        assertFalse(s.strike(Map.of(KnappingPattern.SPEAR_POINT, 4), true));
        assertTrue(s.strike(Map.of(KnappingPattern.SPEAR_POINT, 5), true));
        assertTrue(s.finished());

        KnappingSession beginner = new KnappingSession(Material.FLINT);
        beginner.choose(KnappingPattern.SPEAR_POINT);
        assertTrue(beginner.strike(Map.of(), false));
    }
}
