package dev.paleolithic.core.heat;

import dev.paleolithic.core.Paleolithic;
import dev.paleolithic.rules.heat.BiomeClimate;
import dev.paleolithic.rules.heat.HeatSource;
import dev.paleolithic.rules.heat.Surroundings;
import dev.paleolithic.rules.heat.WetTracker;
import dev.paleolithic.rules.shelter.BlockProbe;
import dev.paleolithic.rules.shelter.ShelterTest;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Reads the world around one player and fills in {@link Surroundings} for the
 * rules (decision D2: the platform layer samples, the rules decide).
 */
public final class HeatSampler {

    public static final TagKey<Block> INSULATING =
            TagKey.create(Registries.BLOCK, Paleolithic.id("insulating"));

    /** Night runs from time 12,542 to 23,460 (spec section 5). */
    static final long NIGHT_START = 12_542;
    static final long NIGHT_END = 23_460;

    /** Search radius for heat sources: the largest vanilla-block outer radius (hearth, 9). */
    static final int HEAT_SCAN_RADIUS = 9;

    private final BiomeClimate climate;

    public HeatSampler(BiomeClimate climate) {
        this.climate = climate;
    }

    public Surroundings sample(ServerPlayer player, WetTracker wet, int ticks) {
        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        BlockPos head = BlockPos.containing(eye);

        Holder<Biome> biome = level.getBiome(player.blockPosition());
        String biomeId = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        double base = climate.baseTemperature(biomeId, biome.value().getBaseTemperature());

        boolean underground = level.getBrightness(LightLayer.SKY, head) == 0;
        boolean exposed = level.canSeeSky(head);

        ShelterTest.Result shelter = ShelterTest.run(probe(level), head.getX(), head.getY(), head.getZ());

        boolean inRain = level.isRainingAt(head) || level.isRainingAt(player.blockPosition());
        Surroundings.Wetness wetness = wet.update(player.isInWater(), player.isEyeInFluid(FluidTags.WATER),
                inRain, false, ticks);

        return new Surroundings(base, underground, isNight(level), weather(level, head), exposed,
                shelter.sheltered(), shelter.insulated(), head.getY(), wetness,
                strongestHeat(level, eye), List.of(), 0);
    }

    static boolean isNight(ServerLevel level) {
        if (level.dimensionType().hasFixedTime()) return false;
        long time = Math.floorMod(level.getDefaultClockTime(), 24_000L);
        return time >= NIGHT_START && time <= NIGHT_END;
    }

    static Surroundings.Weather weather(ServerLevel level, BlockPos head) {
        if (!level.isRaining()) return Surroundings.Weather.CLEAR;
        Biome.Precipitation p = level.getBiome(head).value().getPrecipitationAt(head, level.getSeaLevel());
        if (p == Biome.Precipitation.NONE) return Surroundings.Weather.CLEAR;
        return level.isThundering() ? Surroundings.Weather.THUNDER : Surroundings.Weather.PRECIPITATION;
    }

    static BlockProbe probe(ServerLevel level) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        return new BlockProbe() {
            @Override
            public boolean countsAsShelter(int x, int y, int z) {
                return level.getBlockState(pos.set(x, y, z)).is(BlockTags.BLOCKS_MOTION_NO_LEAVES);
            }

            @Override
            public boolean isInsulating(int x, int y, int z) {
                return level.getBlockState(pos.set(x, y, z)).is(INSULATING);
            }
        };
    }

    /** The strongest single heat source in range, in degrees C (sources do not stack). */
    static double strongestHeat(ServerLevel level, Vec3 eye) {
        BlockPos centre = BlockPos.containing(eye);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        double best = 0;
        int r = HEAT_SCAN_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    pos.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    HeatSource source = sourceOf(level.getBlockState(pos));
                    if (source == null || source.strength() <= best) continue;
                    double ddx = pos.getX() + 0.5 - eye.x, ddy = pos.getY() + 0.5 - eye.y, ddz = pos.getZ() + 0.5 - eye.z;
                    double distance = Math.sqrt(ddx * ddx + ddy * ddy + ddz * ddz);
                    best = Math.max(best, source.contributionAt(distance));
                }
            }
        }
        return best;
    }

    /** Vanilla blocks that give heat. Paleolithic's own sources (hearth, embers, lamp) come with carriers. */
    static HeatSource sourceOf(BlockState state) {
        if (state.isAir()) return null;
        if (state.is(BlockTags.CAMPFIRES)) {
            return state.getValue(CampfireBlock.LIT) ? HeatSource.CAMPFIRE : null;
        }
        if (state.is(Blocks.LAVA)) return HeatSource.LAVA;
        if (state.is(BlockTags.FIRE)) return HeatSource.FIRE;
        if (state.is(Blocks.MAGMA_BLOCK)) return HeatSource.MAGMA_BLOCK;
        if (state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)) return HeatSource.TORCH;
        return null;
    }
}
