package dev.paleolithic.core.creature;

import dev.paleolithic.core.Paleolithic;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Ravager;

/**
 * Turns a vanilla ravager into a woolly mammoth or calf (spec section 20,
 * stand-ins: "Ravager x1.4, calf x0.5"). Nothing new is registered: the
 * client sees a scaled ravager, the server runs the mammoth.
 */
public final class MammothSetup {

    private MammothSetup() {}

    /** Applies stats and AI. Safe to call again on an entity that already has them. */
    public static void apply(Ravager ravager, Species species) {
        GoalSelector goals = MobAccess.goals(ravager);
        boolean done = goals.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof MammothBrain);
        if (done) return;

        ravager.setPersistenceRequired();
        // Silenced: Paleolithic plays its own sounds (the resource pack gives them their voice).
        ravager.setSilent(true);
        boolean fresh = !ravager.entityTags().contains("paleolithic.initialised");
        set(ravager, Attributes.MAX_HEALTH, species.health);
        set(ravager, Attributes.SCALE, species.scale);
        set(ravager, Attributes.ATTACK_DAMAGE, Math.max(species.damage, 0));
        set(ravager, Attributes.KNOCKBACK_RESISTANCE, species.isCalf() ? 0.3 : 0.9);
        set(ravager, Attributes.FOLLOW_RANGE, 40);
        set(ravager, Attributes.STEP_HEIGHT, species.isCalf() ? 1.0 : 1.5);
        if (fresh) {
            ravager.setHealth((float) species.health);
            ravager.addTag("paleolithic.initialised");
        }

        goals.removeAllGoals(g -> true);
        MobAccess.targets(ravager).removeAllGoals(g -> true);
        goals.addGoal(0, new FloatGoal(ravager));
        goals.addGoal(1, new MammothBrain(ravager, species));
        ravager.setTarget(null);
        Paleolithic.LOG.debug("Mammoth set up: {} {}", species, ravager.getUUID());
    }

    private static void set(Ravager r, Holder<Attribute> attribute, double value) {
        AttributeInstance a = r.getAttribute(attribute);
        if (a != null) a.setBaseValue(value);
    }

    /** Detection radius R with the modifiers core can apply today (night). */
    static double detectionRadius(ServerLevel level, Species species) {
        double r = species.detection;
        long t = Math.floorMod(level.getDefaultClockTime(), 24_000L);
        if (!level.dimensionType().hasFixedTime() && t >= 12_542 && t <= 23_460) r *= 0.75;
        return r;
    }
}
