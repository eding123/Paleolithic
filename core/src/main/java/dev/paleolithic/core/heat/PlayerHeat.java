package dev.paleolithic.core.heat;

import dev.paleolithic.rules.heat.BodyHeat;
import dev.paleolithic.rules.heat.FeltTemperature;
import dev.paleolithic.rules.heat.WetTracker;
import net.minecraft.nbt.CompoundTag;

/** One player's heat state: body heat, wetness and the last step, for the HUD. */
public final class PlayerHeat {

    final BodyHeat body = new BodyHeat();
    final WetTracker wet = new WetTracker();
    int ticksUntilStep;
    BodyHeat.StepResult last;
    FeltTemperature.Breakdown lastBreakdown;

    public BodyHeat body() { return body; }
    public BodyHeat.StepResult last() { return last; }
    public FeltTemperature.Breakdown lastBreakdown() { return lastBreakdown; }

    public CompoundTag save() {
        BodyHeat.Snapshot s = body.save();
        CompoundTag tag = new CompoundTag();
        tag.putDouble("value", s.value());
        tag.putInt("secondWind", s.secondWindTicks());
        tag.putInt("hypothermia", s.hypothermiaTicks());
        tag.putInt("stress", s.stressTicks());
        tag.putInt("cool", s.coolTicks());
        tag.putBoolean("overheated", s.overheated());
        tag.putInt("ticksSinceWet", wet.ticksSinceWet());
        return tag;
    }

    public void load(CompoundTag tag) {
        body.load(new BodyHeat.Snapshot(
                tag.getDoubleOr("value", BodyHeat.SPAWN_VALUE),
                tag.getIntOr("secondWind", 0),
                tag.getIntOr("hypothermia", 0),
                tag.getIntOr("stress", 0),
                tag.getIntOr("cool", 0),
                tag.getBooleanOr("overheated", false)));
        wet.load(tag.getIntOr("ticksSinceWet", Integer.MAX_VALUE / 2));
    }
}
