package dev.paleolithic.rules.diet;

/**
 * The lean-diet rule (spec section 7). Lean food adds 1 to the lean count L;
 * fatty or plant food lowers it by 3, to a minimum of 0. At L of 6 or more,
 * food gives half saturation; at 10 or more, also 75% of its hunger value.
 */
public final class LeanDiet {

    public enum Tag { FATTY, LEAN, PLANT }

    public static final int LEAN_DIET_AT = 6;
    public static final int SEVERE_AT = 10;
    public static final int RECOVERY_PER_MEAL = 3;

    /** Hunger and saturation actually granted for one meal. */
    public record Meal(int nutrition, float saturationModifier) {}

    private int leanCount;

    public int leanCount() { return leanCount; }

    public boolean leanDiet() { return leanCount >= LEAN_DIET_AT; }

    public boolean severe() { return leanCount >= SEVERE_AT; }

    /**
     * Apply eating one food. The penalty uses the count before this meal, so a
     * fatty meal eaten while on a Lean Diet is itself penalised but lifts the
     * status afterwards.
     */
    public Meal eat(Tag tag, int nutrition, float saturationModifier) {
        Meal meal = adjust(nutrition, saturationModifier);
        if (tag == Tag.LEAN) {
            leanCount++;
        } else {
            leanCount = Math.max(0, leanCount - RECOVERY_PER_MEAL);
        }
        return meal;
    }

    Meal adjust(int nutrition, float saturationModifier) {
        int n = nutrition;
        float s = saturationModifier;
        if (leanDiet()) s *= 0.5f;
        if (severe()) n = Math.max(1, (int) Math.floor(nutrition * 0.75));
        return new Meal(n, s);
    }

    public void load(int leanCount) {
        this.leanCount = Math.max(0, leanCount);
    }
}
