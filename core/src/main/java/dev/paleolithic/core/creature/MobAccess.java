package dev.paleolithic.core.creature;

import java.lang.reflect.Field;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;

/**
 * Reaches a mob's goal selectors so core can replace a stand-in's AI. The
 * fields are protected in vanilla and their names are the same on every
 * platform because 26.x is unobfuscated (decision D22).
 */
final class MobAccess {

    private static final Field GOALS = field("goalSelector");
    private static final Field TARGETS = field("targetSelector");

    private MobAccess() {}

    static GoalSelector goals(Mob mob) {
        return get(GOALS, mob);
    }

    static GoalSelector targets(Mob mob) {
        return get(TARGETS, mob);
    }

    private static Field field(String name) {
        try {
            Field f = Mob.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            throw new IllegalStateException("Cannot reach Mob." + name + " on this platform", e);
        }
    }

    private static GoalSelector get(Field f, Mob mob) {
        try {
            return (GoalSelector) f.get(mob);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
