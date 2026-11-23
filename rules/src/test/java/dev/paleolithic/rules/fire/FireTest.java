package dev.paleolithic.rules.fire;

import org.junit.Test;

import static org.junit.Assert.*;

public class FireTest {

    private static final int MINUTE = 60 * 20;

    @Test
    public void coldFireMustBeLitBeforeFuelling() {
        Fire f = new Fire(Fire.Kind.CAMPFIRE);
        assertFalse(f.addFuel(Fire.Fuel.LOG));
        assertTrue(f.light());
        assertEquals(4 * MINUTE, f.fuelTicks());
    }

    @Test
    public void fuelIsCappedAt16MinutesForACampfire() {
        Fire f = new Fire(Fire.Kind.CAMPFIRE);
        f.light();
        for (int i = 0; i < 5; i++) f.addFuel(Fire.Fuel.LOG);
        assertEquals(16 * MINUTE, f.fuelTicks());
        assertFalse(f.addFuel(Fire.Fuel.STICK));
    }

    @Test
    public void hearthBurnsAtHalfRateSoALogLasts8Minutes() {
        Fire f = new Fire(Fire.Kind.HEARTH);
        f.light();
        f.tick(8 * MINUTE - 20, Fire.Exposure.COVERED);
        assertEquals(Fire.State.LIT, f.state());
        f.tick(20, Fire.Exposure.COVERED);
        assertEquals(Fire.State.EMBERS, f.state());
        assertEquals(10 * MINUTE, f.emberTicksLeft());
    }

    @Test
    public void weatherSpeedsBurningButNeverPutsItOut() {
        Fire rain = new Fire(Fire.Kind.CAMPFIRE);
        rain.light();
        rain.tick(2 * MINUTE, Fire.Exposure.PRECIPITATION);
        assertEquals(Fire.State.EMBERS, rain.state());

        Fire blizzard = new Fire(Fire.Kind.CAMPFIRE);
        blizzard.light();
        assertEquals(80, blizzard.secondsLeft(Fire.Exposure.BLIZZARD));
    }

    @Test
    public void fuelDuringEmbersRelightsWithoutATool() {
        Fire f = new Fire(Fire.Kind.CAMPFIRE);
        f.light();
        f.tick(4 * MINUTE, Fire.Exposure.COVERED);
        assertEquals(Fire.State.EMBERS, f.state());
        assertTrue(f.addFuel(Fire.Fuel.STICK));
        assertEquals(Fire.State.LIT, f.state());
        assertEquals(30 * 20, f.fuelTicks());
    }

    @Test
    public void coolFireLeavesOneCharcoalPerTwoLogsUpToFour() {
        Fire f = new Fire(Fire.Kind.CAMPFIRE);
        f.light();
        for (int i = 0; i < 3; i++) f.addFuel(Fire.Fuel.LOG);
        f.tick(16 * MINUTE, Fire.Exposure.COVERED);
        f.tick(5 * MINUTE, Fire.Exposure.COVERED);
        assertEquals(Fire.State.COLD, f.state());
        assertEquals(1, f.collectCharcoal().getAsInt());
        assertTrue(f.collectCharcoal().isEmpty());
    }

    @Test
    public void charcoalIsCappedAtFour() {
        Fire f = new Fire(Fire.Kind.HEARTH);
        f.light();
        for (int i = 0; i < 20; i++) {
            f.addFuel(Fire.Fuel.LOG);
            f.tick(8 * MINUTE, Fire.Exposure.COVERED);
        }
        f.tick(200 * MINUTE, Fire.Exposure.COVERED);
        assertEquals(4, f.collectCharcoal().getAsInt());
    }
}
