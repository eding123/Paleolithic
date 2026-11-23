package dev.paleolithic.rules.hunting;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Regional hunting pressure (spec section 12). The world is split into
 * 512 x 512 regions; each counts kills per species. Spawn weight and calf
 * births are multiplied by max(0.2, 1 - K / cap); K falls by 1 per in-game day.
 */
public final class GamePressure {

    public static final int REGION_SIZE = 512;
    public static final double FLOOR = 0.2;
    public static final long TICKS_PER_DAY = 24_000;
    public static final double THAW_CAP_MULTIPLIER = 1.5;

    public record RegionKey(int rx, int rz) {
        public static RegionKey ofBlock(int x, int z) {
            return new RegionKey(Math.floorDiv(x, REGION_SIZE), Math.floorDiv(z, REGION_SIZE));
        }
    }

    public enum Message { WARY, SCARCE }

    private static final class Counter {
        double kills;
        long lastUpdate;
    }

    private final Map<RegionKey, Map<String, Counter>> regions = new HashMap<>();
    private boolean thaw;

    public void setThaw(boolean thaw) { this.thaw = thaw; }

    public double effectiveCap(double cap) {
        return thaw ? cap * THAW_CAP_MULTIPLIER : cap;
    }

    /** Current kill count after decay. */
    public double kills(RegionKey region, String species, long gameTime) {
        Counter c = counter(region, species, false);
        if (c == null) return 0;
        decay(c, gameTime);
        return c.kills;
    }

    /**
     * Record a player or tamed-wolf kill. Returns a message to show the killer
     * when the count crosses 50% or 100% of the cap.
     */
    public Optional<Message> recordKill(RegionKey region, String species, double cap, long gameTime) {
        Counter c = counter(region, species, true);
        decay(c, gameTime);
        double effCap = effectiveCap(cap);
        double before = c.kills;
        c.kills += 1;
        if (before < effCap && c.kills >= effCap) return Optional.of(Message.SCARCE);
        if (before < effCap / 2 && c.kills >= effCap / 2) return Optional.of(Message.WARY);
        return Optional.empty();
    }

    /** Multiplier for natural spawn weight and calf births. */
    public double spawnMultiplier(RegionKey region, String species, double cap, long gameTime) {
        double k = kills(region, species, gameTime);
        return Math.max(FLOOR, 1 - k / effectiveCap(cap));
    }

    /** Calves are born only while pressure is below 50% of the cap. */
    public boolean calvesAllowed(RegionKey region, String species, double cap, long gameTime) {
        return kills(region, species, gameTime) < effectiveCap(cap) / 2;
    }

    private Counter counter(RegionKey region, String species, boolean create) {
        Map<String, Counter> m = create ? regions.computeIfAbsent(region, r -> new HashMap<>()) : regions.get(region);
        if (m == null) return null;
        return create ? m.computeIfAbsent(species, s -> new Counter()) : m.get(species);
    }

    private static void decay(Counter c, long gameTime) {
        if (gameTime > c.lastUpdate) {
            c.kills = Math.max(0, c.kills - (gameTime - c.lastUpdate) / (double) TICKS_PER_DAY);
        }
        c.lastUpdate = Math.max(c.lastUpdate, gameTime);
    }

    // --- persistence -----------------------------------------------------

    public record Entry(int rx, int rz, String species, double kills, long lastUpdate) {}

    public java.util.List<Entry> save() {
        java.util.List<Entry> out = new java.util.ArrayList<>();
        regions.forEach((key, m) -> m.forEach((species, c) -> {
            if (c.kills > 0) out.add(new Entry(key.rx(), key.rz(), species, c.kills, c.lastUpdate));
        }));
        return out;
    }

    public void load(Iterable<Entry> entries) {
        regions.clear();
        for (Entry e : entries) {
            Counter c = counter(new RegionKey(e.rx(), e.rz()), e.species(), true);
            c.kills = e.kills();
            c.lastUpdate = e.lastUpdate();
        }
    }
}
