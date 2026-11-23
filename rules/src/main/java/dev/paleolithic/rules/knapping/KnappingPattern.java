package dev.paleolithic.rules.knapping;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The 5x5 knapping patterns (Items and Recipes tab, "Knapping patterns").
 * '#' is a locked head cell; '.' is waste the player chips away.
 */
public enum KnappingPattern {
    HAND_AXE("hand_axe", 1,
            "..#..",
            ".###.",
            ".###.",
            "#####",
            ".###."),
    KNIFE_BLADE("knife_blade", 1,
            "..#..",
            "..#..",
            ".##..",
            ".##..",
            ".##.."),
    SCRAPER("scraper", 1,
            ".....",
            ".###.",
            "#####",
            "#####",
            "....."),
    SPEAR_POINT("spear_point", 1,
            "..#..",
            ".###.",
            ".###.",
            ".###.",
            "..#.."),
    AXE_HEAD("axe_head", 1,
            "###..",
            "####.",
            "#####",
            "####.",
            "###.."),
    PICK_HEAD("pick_head", 1,
            ".###.",
            "##.##",
            "#...#",
            ".....",
            "....."),
    SPADE_HEAD("spade_head", 1,
            ".###.",
            ".###.",
            ".###.",
            "..#..",
            "....."),
    DART_POINT("dart_point", 2,
            "..#..",
            ".###.",
            "..#..",
            "..#..",
            ".....");

    public static final int SIZE = 5;

    public final String id;
    public final int yield;
    private final boolean[] head = new boolean[SIZE * SIZE];

    KnappingPattern(String id, int yield, String... rows) {
        if (rows.length != SIZE) throw new IllegalArgumentException(id + ": need 5 rows");
        this.id = id;
        this.yield = yield;
        for (int r = 0; r < SIZE; r++) {
            if (rows[r].length() != SIZE) throw new IllegalArgumentException(id + ": row " + r);
            for (int c = 0; c < SIZE; c++) {
                char ch = rows[r].charAt(c);
                if (ch != '#' && ch != '.') throw new IllegalArgumentException(id + ": bad char " + ch);
                head[r * SIZE + c] = ch == '#';
            }
        }
    }

    /** True if the cell (row-major index 0-24) is part of the tool head. */
    public boolean isHead(int cell) {
        return head[cell];
    }

    /** Number of clicks needed: the waste cells. */
    public int wasteCells() {
        int n = 0;
        for (boolean h : head) if (!h) n++;
        return n;
    }

    /** Which patterns each knappable stone offers (spec section 10). */
    public static Set<KnappingPattern> forMaterial(Material material) {
        return switch (material) {
            case FLINT -> EnumSet.allOf(KnappingPattern.class);
            case HEAT_TREATED_FLINT -> EnumSet.of(KNIFE_BLADE, SPEAR_POINT, AXE_HEAD, PICK_HEAD, SPADE_HEAD,
                    DART_POINT);
            case OBSIDIAN -> EnumSet.of(KNIFE_BLADE, SPEAR_POINT, DART_POINT);
        };
    }

    public enum Material {
        FLINT("flint"), HEAT_TREATED_FLINT("fine"), OBSIDIAN("obsidian");

        /** Prefix of the result item id, e.g. "fine" + "_" + "spear_point". */
        public final String resultPrefix;

        Material(String resultPrefix) { this.resultPrefix = resultPrefix; }
    }

    /** Result item id for a material and pattern, e.g. {@code paleolithic:obsidian_dart_point}. */
    public String resultId(Material material) {
        return "paleolithic:" + material.resultPrefix + "_" + id;
    }

    public static List<KnappingPattern> ordered() {
        return List.of(values());
    }
}
