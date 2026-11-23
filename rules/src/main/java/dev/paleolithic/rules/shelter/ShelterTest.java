package dev.paleolithic.rules.shelter;

/**
 * The shelter test (spec section 8): a roof within 8 blocks above the head,
 * and at least 3 of 4 horizontal rays from head height hitting a wall within
 * 5 blocks. Insulated when at least 3 of the blocks hit are insulating.
 * Costs at most 8 + 4 x 5 = 28 block checks.
 */
public final class ShelterTest {

    public static final int ROOF_SCAN = 8;
    public static final int WALL_SCAN = 5;
    public static final int WALLS_NEEDED = 3;
    public static final int INSULATING_HITS_NEEDED = 3;

    private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}; // N, E, S, W

    private ShelterTest() {}

    public record Result(boolean sheltered, boolean insulated, boolean roof, int walls, int insulatingHits) {
        public static final Result NONE = new Result(false, false, false, 0, 0);
    }

    /**
     * @param headX block X of the player's head
     * @param headY block Y of the player's head
     * @param headZ block Z of the player's head
     */
    public static Result run(BlockProbe probe, int headX, int headY, int headZ) {
        int insulatingHits = 0;

        boolean roof = false;
        for (int dy = 1; dy <= ROOF_SCAN; dy++) {
            if (probe.countsAsShelter(headX, headY + dy, headZ)) {
                roof = true;
                if (probe.isInsulating(headX, headY + dy, headZ)) insulatingHits++;
                break;
            }
        }
        if (!roof) return new Result(false, false, false, 0, 0);

        int walls = 0;
        for (int[] d : DIRECTIONS) {
            for (int step = 1; step <= WALL_SCAN; step++) {
                int x = headX + d[0] * step;
                int z = headZ + d[1] * step;
                if (probe.countsAsShelter(x, headY, z)) {
                    walls++;
                    if (probe.isInsulating(x, headY, z)) insulatingHits++;
                    break;
                }
            }
        }

        boolean sheltered = walls >= WALLS_NEEDED;
        boolean insulated = sheltered && insulatingHits >= INSULATING_HITS_NEEDED;
        return new Result(sheltered, insulated, true, walls, insulatingHits);
    }
}
