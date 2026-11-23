package dev.paleolithic.rules.shelter;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ShelterTestTest {

    private enum B { STONE, LEAVES, THATCH }

    private static final class World implements BlockProbe {
        final Map<String, B> blocks = new HashMap<>();
        int checks;

        World set(int x, int y, int z, B b) {
            blocks.put(x + "," + y + "," + z, b);
            return this;
        }

        @Override
        public boolean countsAsShelter(int x, int y, int z) {
            checks++;
            B b = blocks.get(x + "," + y + "," + z);
            return b != null && b != B.LEAVES;
        }

        @Override
        public boolean isInsulating(int x, int y, int z) {
            return blocks.get(x + "," + y + "," + z) == B.THATCH;
        }
    }

    private static World hut(B roof, B wall, int wallCount) {
        World w = new World().set(0, 4, 0, roof);
        int[][] dirs = {{0, -2}, {2, 0}, {0, 2}, {-2, 0}};
        for (int i = 0; i < wallCount; i++) w.set(dirs[i][0], 1, dirs[i][1], wall);
        return w;
    }

    @Test
    public void roofAndThreeWallsIsSheltered() {
        ShelterTest.Result r = ShelterTest.run(hut(B.STONE, B.STONE, 3), 0, 1, 0);
        assertTrue(r.sheltered());
        assertFalse(r.insulated());
    }

    @Test
    public void twoWallsIsNotEnough() {
        assertFalse(ShelterTest.run(hut(B.STONE, B.STONE, 2), 0, 1, 0).sheltered());
    }

    @Test
    public void leavesAreNeverARoof() {
        World w = hut(B.LEAVES, B.STONE, 4);
        assertFalse(ShelterTest.run(w, 0, 1, 0).sheltered());
    }

    @Test
    public void wallsBeyondFiveBlocksDoNotCount() {
        World w = new World().set(0, 4, 0, B.STONE).set(6, 1, 0, B.STONE).set(-6, 1, 0, B.STONE)
                .set(0, 1, 6, B.STONE).set(0, 1, -6, B.STONE);
        assertFalse(ShelterTest.run(w, 0, 1, 0).sheltered());
    }

    @Test
    public void threeInsulatingHitsMakeItInsulated() {
        World w = hut(B.THATCH, B.STONE, 4).set(0, 1, -2, B.THATCH).set(2, 1, 0, B.THATCH);
        ShelterTest.Result r = ShelterTest.run(w, 0, 1, 0);
        assertTrue(r.insulated());
        assertEquals(3, r.insulatingHits());
    }

    @Test
    public void neverMoreThan28Checks() {
        World open = new World();
        ShelterTest.run(open, 0, 1, 0);
        assertTrue(open.checks <= 28);
        World roofOnly = new World().set(0, 9, 0, B.STONE);
        ShelterTest.run(roofOnly, 0, 1, 0);
        assertTrue("checks: " + roofOnly.checks, roofOnly.checks <= 28);
    }
}
