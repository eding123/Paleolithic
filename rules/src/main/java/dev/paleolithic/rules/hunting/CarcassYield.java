package dev.paleolithic.rules.hunting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A carcass's yield and how it splits into cuts (spec section 12 and the
 * Bestiary's "Carcass yields" table). Yields are listed in cut order, so an
 * interrupted butchering still gets the hide and meat first.
 */
public record CarcassYield(String species, int cuts, List<Stack> items) {

    public enum Category { MEAT, FAT, HIDE, OTHER }

    public record Stack(String itemId, int count, Category category) {}

    /** Item counts for one cut. */
    public record Cut(Map<String, Integer> items) {}

    public enum Tool {
        KNIFE(1.0, 20), HAND_AXE(0.75, 30);

        public final double yield;
        public final int ticksPerCut;

        Tool(double yield, int ticksPerCut) {
            this.yield = yield;
            this.ticksPerCut = ticksPerCut;
        }
    }

    public static final int PRACTICED_KNIFE_TICKS = 15;
    public static final int PRACTICED_AFTER = 10;
    public static final int LIFETIME_TICKS = 10 * 60 * 20;
    public static final int SCAVENGER_TICKS_PER_CUT = 5 * 20;
    public static final double PRIME_CHANCE = 0.10;

    public CarcassYield {
        items = List.copyOf(items);
        if (cuts <= 0) throw new IllegalArgumentException("cuts must be positive");
    }

    public static int ticksPerCut(Tool tool, int carcassesButchered) {
        if (tool == Tool.KNIFE && carcassesButchered >= PRACTICED_AFTER) return PRACTICED_KNIFE_TICKS;
        return tool.ticksPerCut;
    }

    /**
     * Total counts after modifiers. The hand axe gives 75% of each item,
     * rounded down with a minimum of 1. A prime carcass gives 50% more meat
     * and fat, rounded up, and double hides and pelts. Legends double all.
     */
    public List<Stack> totals(Tool tool, boolean prime, boolean legend) {
        List<Stack> out = new ArrayList<>();
        for (Stack s : items) {
            double n = s.count();
            if (legend) n *= 2;
            if (prime) {
                if (s.category() == Category.MEAT || s.category() == Category.FAT) n = Math.ceil(n * 1.5);
                if (s.category() == Category.HIDE) n *= 2;
            }
            if (tool.yield < 1.0) n = Math.max(1, Math.floor(n * tool.yield));
            out.add(new Stack(s.itemId(), (int) n, s.category()));
        }
        return out;
    }

    /**
     * Split totals into {@link #cuts()} cuts as evenly as possible, keeping
     * item order. Legends have twice the cuts.
     */
    public List<Cut> plan(Tool tool, boolean prime, boolean legend) {
        List<String> queue = new ArrayList<>();
        for (Stack s : totals(tool, prime, legend)) {
            for (int i = 0; i < s.count(); i++) queue.add(s.itemId());
        }
        int n = legend ? cuts * 2 : cuts;
        List<Cut> out = new ArrayList<>(n);
        int total = queue.size();
        int index = 0;
        for (int c = 0; c < n; c++) {
            int size = total / n + (c < total % n ? 1 : 0);
            Map<String, Integer> cut = new LinkedHashMap<>();
            for (int k = 0; k < size; k++) cut.merge(queue.get(index++), 1, Integer::sum);
            out.add(new Cut(cut));
        }
        return out;
    }
}
