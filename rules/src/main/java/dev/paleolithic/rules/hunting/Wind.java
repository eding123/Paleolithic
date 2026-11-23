package dev.paleolithic.rules.hunting;

import java.util.random.RandomGenerator;

/**
 * The world's single wind direction (spec section 12): one of 8 compass
 * points, shifting by up to 90 degrees every 6-12 minutes.
 */
public final class Wind {

    public static final int MIN_CHANGE_TICKS = 6 * 60 * 20;
    public static final int MAX_CHANGE_TICKS = 12 * 60 * 20;

    /** Directions the wind blows toward, clockwise from north. */
    public enum Direction {
        N(0, -1), NE(1, -1), E(1, 0), SE(1, 1), S(0, 1), SW(-1, 1), W(-1, 0), NW(-1, -1);

        public final double dx;
        public final double dz;

        Direction(int dx, int dz) {
            double len = Math.hypot(dx, dz);
            this.dx = dx / len;
            this.dz = dz / len;
        }

        public Direction rotate(int steps) {
            Direction[] v = values();
            return v[Math.floorMod(ordinal() + steps, v.length)];
        }
    }

    private Direction direction;
    private long nextChangeTick;

    public Wind(Direction direction, long nextChangeTick) {
        this.direction = direction;
        this.nextChangeTick = nextChangeTick;
    }

    public Direction direction() { return direction; }
    public long nextChangeTick() { return nextChangeTick; }

    /** Call with the current game time. Returns true if the direction changed. */
    public boolean update(long gameTime, RandomGenerator random) {
        if (gameTime < nextChangeTick) return false;
        int steps = random.nextInt(-2, 3); // -90 to +90 degrees in 45-degree steps
        Direction old = direction;
        direction = direction.rotate(steps);
        nextChangeTick = gameTime + random.nextInt(MIN_CHANGE_TICKS, MAX_CHANGE_TICKS + 1);
        return direction != old;
    }
}
