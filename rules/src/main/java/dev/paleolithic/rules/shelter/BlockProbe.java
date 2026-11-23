package dev.paleolithic.rules.shelter;

/**
 * Read-only view of the blocks around a player, supplied by the platform layer.
 */
public interface BlockProbe {

    /** True if the block at this position blocks movement and is not leaves. */
    boolean countsAsShelter(int x, int y, int z);

    /** True if the block is in the {@code paleolithic:insulating} tag. */
    boolean isInsulating(int x, int y, int z);
}
