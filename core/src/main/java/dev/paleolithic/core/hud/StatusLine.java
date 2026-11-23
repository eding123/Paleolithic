package dev.paleolithic.core.hud;

import dev.paleolithic.rules.heat.BodyHeat;
import dev.paleolithic.rules.heat.HeatState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * The vanilla-client HUD (spec section 17): body heat as the experience bar
 * fill with the level number hidden, and a status line in the action bar.
 * Icons are Unicode fallbacks; the resource pack will replace them with glyphs.
 */
public final class StatusLine {

    private StatusLine() {}

    public static void show(ServerPlayer player, BodyHeat.StepResult step) {
        // Level 0 hides the number. Vanilla only resends experience when the
        // player's own experience changes, so this fill stays until the next send.
        player.connection.send(new ClientboundSetExperiencePacket(fill(step.bodyHeat()), 0, 0));
        player.sendOverlayMessage(line(step));
    }

    public static float fill(double bodyHeat) {
        return (float) Math.max(0, Math.min(1, bodyHeat / BodyHeat.MAX));
    }

    public static Component line(BodyHeat.StepResult step) {
        return Component.literal(text(step)).withStyle(color(step.state()));
    }

    /** The plain text of the status line, also used by tests and logs. */
    public static String text(BodyHeat.StepResult step) {
        StringBuilder s = new StringBuilder();
        s.append(icon(step.state())).append(' ').append(label(step.state()));
        String arrows = arrows(step.trend());
        if (!arrows.isEmpty()) s.append(' ').append(arrows);
        if (step.overheated()) s.append("  Overheated");
        return s.toString();
    }

    /** Shape-coded so the state reads without color. */
    static String icon(HeatState state) {
        return switch (state) {
            case WARM -> "♨";                  // flame (hot springs sign)
            case CHILLY -> "✹";                // ember (twelve-pointed star)
            case COLD -> "❄";                  // snowflake
            case FREEZING, HYPOTHERMIC -> "✦"; // cracked ice (four-pointed star)
        };
    }

    static String label(HeatState state) {
        return switch (state) {
            case WARM -> "Warm";
            case CHILLY -> "Chilly";
            case COLD -> "Cold";
            case FREEZING -> "Freezing";
            case HYPOTHERMIC -> "Hypothermic";
        };
    }

    static String arrows(BodyHeat.Trend trend) {
        return switch (trend) {
            case WARMING -> "▲";
            case COOLING -> "▼";
            case COOLING_FAST -> "▼▼";
            case STABLE -> "";
        };
    }

    static ChatFormatting color(HeatState state) {
        return switch (state) {
            case WARM -> ChatFormatting.GOLD;
            case CHILLY -> ChatFormatting.YELLOW;
            case COLD -> ChatFormatting.AQUA;
            case FREEZING -> ChatFormatting.BLUE;
            case HYPOTHERMIC -> ChatFormatting.WHITE;
        };
    }
}
