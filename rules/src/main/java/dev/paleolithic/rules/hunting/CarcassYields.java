package dev.paleolithic.rules.hunting;

import dev.paleolithic.rules.hunting.CarcassYield.Category;
import dev.paleolithic.rules.hunting.CarcassYield.Stack;

import java.util.List;
import java.util.Map;

/**
 * Default carcass yields from the Bestiary. The core module loads the same
 * tables from data so packs can override them; these defaults back the tests.
 */
public final class CarcassYields {

    private CarcassYields() {}

    private static Stack s(String item, int count, Category c) {
        return new Stack(item.contains(":") ? item : "paleolithic:" + item, count, c);
    }

    public static final Map<String, CarcassYield> DEFAULTS = Map.ofEntries(
            entry("woolly_mammoth", 12,
                    s("raw_pelt", 3, Category.HIDE), s("raw_mammoth", 9, Category.MEAT),
                    s("animal_fat", 5, Category.FAT), s("sinew", 2, Category.OTHER),
                    s("large_bone", 4, Category.OTHER), s("minecraft:bone", 4, Category.OTHER),
                    s("ivory_tusk", 2, Category.OTHER)),
            entry("mammoth_calf", 4,
                    s("raw_pelt", 1, Category.HIDE), s("raw_mammoth", 3, Category.MEAT),
                    s("animal_fat", 1, Category.FAT), s("minecraft:bone", 2, Category.OTHER)),
            entry("woolly_rhino", 8,
                    s("raw_hide", 2, Category.HIDE), s("raw_rhino", 6, Category.MEAT),
                    s("animal_fat", 2, Category.FAT), s("sinew", 2, Category.OTHER),
                    s("large_bone", 2, Category.OTHER), s("minecraft:bone", 2, Category.OTHER),
                    s("horn", 1, Category.OTHER)),
            entry("steppe_bison", 6,
                    s("raw_hide", 2, Category.HIDE), s("raw_bison", 5, Category.MEAT),
                    s("animal_fat", 1, Category.FAT), s("sinew", 2, Category.OTHER),
                    s("large_bone", 1, Category.OTHER), s("minecraft:bone", 2, Category.OTHER)),
            entry("aurochs", 6,
                    s("raw_hide", 2, Category.HIDE), s("minecraft:beef", 4, Category.MEAT),
                    s("animal_fat", 1, Category.FAT), s("sinew", 1, Category.OTHER),
                    s("minecraft:bone", 2, Category.OTHER), s("horn", 1, Category.OTHER)),
            entry("musk_ox", 6,
                    s("raw_pelt", 2, Category.HIDE), s("minecraft:mutton", 4, Category.MEAT),
                    s("animal_fat", 2, Category.FAT), s("sinew", 1, Category.OTHER),
                    s("minecraft:bone", 2, Category.OTHER), s("horn", 1, Category.OTHER)),
            entry("giant_deer", 5,
                    s("raw_hide", 1, Category.HIDE), s("raw_venison", 3, Category.MEAT),
                    s("sinew", 1, Category.OTHER), s("minecraft:bone", 2, Category.OTHER),
                    s("antler", 2, Category.OTHER)),
            entry("wild_horse", 5,
                    s("raw_hide", 1, Category.HIDE), s("raw_horse", 4, Category.MEAT),
                    s("animal_fat", 1, Category.FAT), s("sinew", 1, Category.OTHER),
                    s("minecraft:bone", 2, Category.OTHER)),
            entry("polar_bear", 5,
                    s("raw_pelt", 2, Category.HIDE), s("raw_bear", 3, Category.MEAT),
                    s("animal_fat", 2, Category.FAT), s("minecraft:bone", 2, Category.OTHER)),
            entry("cave_bear", 7,
                    s("raw_pelt", 2, Category.HIDE), s("raw_bear", 5, Category.MEAT),
                    s("animal_fat", 3, Category.FAT), s("sinew", 1, Category.OTHER),
                    s("large_bone", 1, Category.OTHER), s("minecraft:bone", 3, Category.OTHER)),
            entry("cave_lion", 4,
                    s("raw_pelt", 1, Category.HIDE), s("sinew", 1, Category.OTHER),
                    s("minecraft:bone", 2, Category.OTHER)),
            entry("scimitar_cat", 4,
                    s("raw_pelt", 1, Category.HIDE), s("sinew", 1, Category.OTHER),
                    s("minecraft:bone", 2, Category.OTHER)));

    private static Map.Entry<String, CarcassYield> entry(String species, int cuts, Stack... stacks) {
        return Map.entry(species, new CarcassYield(species, cuts, List.of(stacks)));
    }
}
