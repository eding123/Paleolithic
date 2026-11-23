package dev.paleolithic.core.hud;

import static org.junit.Assert.assertEquals;

import dev.paleolithic.rules.heat.BodyHeat;
import dev.paleolithic.rules.heat.HeatState;
import org.junit.Test;

public class StatusLineTest {

    private static BodyHeat.StepResult step(double b, BodyHeat.Trend trend, boolean overheated) {
        return new BodyHeat.StepResult(b, HeatState.of(b), trend, 0, 0, overheated);
    }

    @Test
    public void fillIsBodyHeatOverHundredClamped() {
        assertEquals(0.75f, StatusLine.fill(75), 1e-6);
        assertEquals(0f, StatusLine.fill(-5), 1e-6);
        assertEquals(1f, StatusLine.fill(140), 1e-6);
    }

    @Test
    public void lineShowsIconStateAndTrend() {
        assertEquals("♨ Warm ▼▼", StatusLine.text(step(75, BodyHeat.Trend.COOLING_FAST, false)));
        assertEquals("✹ Chilly ▼", StatusLine.text(step(55, BodyHeat.Trend.COOLING, false)));
        assertEquals("❄ Cold ▲", StatusLine.text(step(30, BodyHeat.Trend.WARMING, false)));
        assertEquals("✦ Freezing", StatusLine.text(step(10, BodyHeat.Trend.STABLE, false)));
        assertEquals("♨ Warm  Overheated", StatusLine.text(step(100, BodyHeat.Trend.STABLE, true)));
    }

    @Test
    public void everyStateHasADistinctShapeExceptTheTwoIceStates() {
        assertEquals(StatusLine.icon(HeatState.FREEZING), StatusLine.icon(HeatState.HYPOTHERMIC));
        assertEquals(4, java.util.Set.of(StatusLine.icon(HeatState.WARM), StatusLine.icon(HeatState.CHILLY),
                StatusLine.icon(HeatState.COLD), StatusLine.icon(HeatState.FREEZING)).size());
    }
}
