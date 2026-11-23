package dev.paleolithic.core.creature;

/**
 * Creature stats from the Bestiary. Speeds are blocks per second; damage is
 * at Normal difficulty in half-hearts.
 */
public enum Species {
    MAMMOTH("mammoth", 120, 14, 2.5, 6.0, 1.4, 20),
    MAMMOTH_CALF("mammoth_calf", 30, 0, 2.5, 6.0, 0.5, 20);

    public final String id;
    public final double health;
    public final double damage;
    public final double walkSpeed;
    public final double runSpeed;
    /** Scale of the stand-in entity (spec section 20, stand-ins table). */
    public final double scale;
    /** Detection radius R. */
    public final double detection;

    Species(String id, double health, double damage, double walkSpeed, double runSpeed, double scale,
            double detection) {
        this.id = id;
        this.health = health;
        this.damage = damage;
        this.walkSpeed = walkSpeed;
        this.runSpeed = runSpeed;
        this.scale = scale;
        this.detection = detection;
    }

    /** Scoreboard tag that marks an entity as this species; survives saving. */
    public String tag() {
        return "paleolithic." + id;
    }

    public boolean isCalf() {
        return this == MAMMOTH_CALF;
    }
}
