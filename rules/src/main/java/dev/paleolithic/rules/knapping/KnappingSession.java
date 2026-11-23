package dev.paleolithic.rules.knapping;

import java.util.Map;
import java.util.Optional;

/**
 * One open knapping menu (spec section 10). The player picks a pattern, the
 * head cells lock, and only waste cells can be chipped, so a piece can never
 * be ruined. The server validates every click through this class.
 */
public final class KnappingSession {

    public static final int PRACTICED_AFTER = 5;

    public enum ClickResult { REMOVED, LOCKED, ALREADY_REMOVED, NO_PATTERN, OUT_OF_RANGE, FINISHED }

    /** A finished head: the item id and how many the player receives. */
    public record Output(String itemId, int count) {}

    private final KnappingPattern.Material material;
    private final boolean[] removed = new boolean[KnappingPattern.SIZE * KnappingPattern.SIZE];
    private KnappingPattern pattern;
    private boolean finished;

    public KnappingSession(KnappingPattern.Material material) {
        this.material = material;
    }

    public KnappingPattern.Material material() { return material; }
    public Optional<KnappingPattern> pattern() { return Optional.ofNullable(pattern); }
    public boolean finished() { return finished; }
    public boolean isRemoved(int cell) { return removed[cell]; }

    /**
     * Choose (or change) the pattern. Changing is allowed only while no cell
     * that the new pattern needs has been chipped away, so a choice can be
     * corrected without wasting stone.
     */
    public boolean choose(KnappingPattern p) {
        if (finished || !KnappingPattern.forMaterial(material).contains(p)) return false;
        for (int i = 0; i < removed.length; i++) {
            if (removed[i] && p.isHead(i)) return false;
        }
        pattern = p;
        return true;
    }

    public ClickResult click(int cell) {
        if (finished) return ClickResult.FINISHED;
        if (cell < 0 || cell >= removed.length) return ClickResult.OUT_OF_RANGE;
        if (pattern == null) return ClickResult.NO_PATTERN;
        if (pattern.isHead(cell)) return ClickResult.LOCKED;
        if (removed[cell]) return ClickResult.ALREADY_REMOVED;
        removed[cell] = true;
        if (allWasteRemoved()) finished = true;
        return ClickResult.REMOVED;
    }

    /** "Strike": finish instantly. Allowed once the pattern is practiced, or when the minigame is off. */
    public boolean strike(Map<KnappingPattern, Integer> completions, boolean minigameEnabled) {
        if (finished || pattern == null) return false;
        if (minigameEnabled && completions.getOrDefault(pattern, 0) < PRACTICED_AFTER) return false;
        for (int i = 0; i < removed.length; i++) if (!pattern.isHead(i)) removed[i] = true;
        finished = true;
        return true;
    }

    /** The output, once finished. The caller consumes one stone and records the completion. */
    public Optional<Output> output() {
        if (!finished) return Optional.empty();
        return Optional.of(new Output(pattern.resultId(material), pattern.yield));
    }

    private boolean allWasteRemoved() {
        for (int i = 0; i < removed.length; i++) {
            if (!pattern.isHead(i) && !removed[i]) return false;
        }
        return true;
    }
}
