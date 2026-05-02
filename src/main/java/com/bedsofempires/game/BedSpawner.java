package com.bedsofempires.game;

import com.bedsofempires.BedsOfEmpires;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

import java.util.*;

public class BedSpawner {

    private static final Block[] BED_COLORS = {
            Blocks.WHITE_BED, Blocks.ORANGE_BED, Blocks.MAGENTA_BED, Blocks.LIGHT_BLUE_BED,
            Blocks.YELLOW_BED, Blocks.LIME_BED, Blocks.PINK_BED, Blocks.GRAY_BED,
            Blocks.LIGHT_GRAY_BED, Blocks.CYAN_BED, Blocks.PURPLE_BED, Blocks.BLUE_BED,
            Blocks.BROWN_BED, Blocks.GREEN_BED, Blocks.RED_BED, Blocks.BLACK_BED
    };

    public static Block getBedByColorName(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "white" -> Blocks.WHITE_BED;
            case "orange" -> Blocks.ORANGE_BED;
            case "magenta" -> Blocks.MAGENTA_BED;
            case "light_blue" -> Blocks.LIGHT_BLUE_BED;
            case "yellow" -> Blocks.YELLOW_BED;
            case "lime" -> Blocks.LIME_BED;
            case "pink" -> Blocks.PINK_BED;
            case "gray" -> Blocks.GRAY_BED;
            case "light_gray" -> Blocks.LIGHT_GRAY_BED;
            case "cyan" -> Blocks.CYAN_BED;
            case "purple" -> Blocks.PURPLE_BED;
            case "blue" -> Blocks.BLUE_BED;
            case "brown" -> Blocks.BROWN_BED;
            case "green" -> Blocks.GREEN_BED;
            case "red" -> Blocks.RED_BED;
            case "black" -> Blocks.BLACK_BED;
            default -> Blocks.RED_BED;
        };
    }

    public static List<String> getAllColorNames() {
        return List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
                "pink", "gray", "light_gray", "cyan", "purple", "blue",
                "brown", "green", "red", "black");
    }

    public static Block getRandomBedColor(RandomSource random, Set<Block> usedColors) {
        List<Block> available = new ArrayList<>();
        for (Block bed : BED_COLORS) {
            if (!usedColors.contains(bed)) {
                available.add(bed);
            }
        }
        if (available.isEmpty()) {
            return BED_COLORS[random.nextInt(BED_COLORS.length)];
        }
        return available.get(random.nextInt(available.size()));
    }

    public static Map<UUID, BlockPos> spawnBeds(
            ServerLevel level,
            List<UUID> players,
            Map<UUID, String> playerColors,
            BedRegistry registry,
            GameSettings settings
    ) {
        Map<UUID, BlockPos> result = new HashMap<>();
        Set<Block> usedColors = new HashSet<>();
        RandomSource random = level.getRandom();
        int searchRadius = Math.max(100000, players.size() * 50000 * settings.getSearchRadiusScale());
        int minDistance = settings.getBedDistance();
        List<BlockPos> placedPositions = new ArrayList<>();

        for (UUID playerId : players) {
            BlockPos bedPos = findValidBedLocation(level, random, searchRadius, minDistance, placedPositions);
            if (bedPos == null) {
                BedsOfEmpires.LOGGER.error("Could not find valid bed location for player {}", playerId);
                continue;
            }

            String colorName = playerColors.get(playerId);
            Block bedBlock;
            if (colorName != null) {
                bedBlock = getBedByColorName(colorName);
            } else {
                bedBlock = getRandomBedColor(random, usedColors);
            }
            usedColors.add(bedBlock);

            clearAreaAroundBed(level, bedPos, settings.getProtectionRadius());
            if (placeBed(level, bedPos, bedBlock)) {
                registry.registerBed(bedPos, playerId);
                placedPositions.add(bedPos);
                result.put(playerId, bedPos);
            }
        }
        return result;
    }

    private static BlockPos findValidBedLocation(
            ServerLevel level,
            RandomSource random,
            int searchRadius,
            int minDistance,
            List<BlockPos> existingBeds
    ) {
        for (int attempt = 0; attempt < 500; attempt++) {
            int x = random.nextInt(searchRadius * 2) - searchRadius;
            int z = random.nextInt(searchRadius * 2) - searchRadius;

            BlockPos surfacePos = findSurface(level, x, z);
            if (surfacePos == null) continue;

            BlockPos headPos = surfacePos.relative(Direction.SOUTH);
            BlockPos headSurface = findSurface(level, headPos.getX(), headPos.getZ());
            if (headSurface == null || headSurface.getY() != surfacePos.getY()) continue;

            if (!isFarEnough(surfacePos, existingBeds, minDistance)) continue;

            return surfacePos;
        }
        return null;
    }

    private static BlockPos findSurface(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        BlockPos surfacePos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
        BlockPos below = surfacePos.below();
        BlockState groundState = level.getBlockState(below);
        if (!groundState.isSolidRender(level, below)) return null;
        if (level.getBlockState(surfacePos).isAir() && level.getBlockState(surfacePos.above()).isAir()) {
            return surfacePos;
        }
        return null;
    }

    private static boolean isFarEnough(BlockPos pos, List<BlockPos> existing, int minDistance) {
        for (BlockPos other : existing) {
            double dist = Math.sqrt(pos.distSqr(other));
            if (dist < minDistance) return false;
        }
        return true;
    }

    private static void clearAreaAroundBed(ServerLevel level, BlockPos bedPos, int radius) {
        int bedY = bedPos.getY();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius) continue;
                int bx = bedPos.getX() + x;
                int bz = bedPos.getZ() + z;
                level.getChunk(bx >> 4, bz >> 4);
                // Iron block floor at bed level - 1
                level.setBlock(new BlockPos(bx, bedY - 1, bz),
                        Blocks.IRON_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
                // Clear everything above the floor
                for (int y = bedY; y < bedY + 10; y++) {
                    BlockPos clearPos = new BlockPos(bx, y, bz);
                    BlockState state = level.getBlockState(clearPos);
                    if (!state.isAir() && !(state.getBlock() instanceof BedBlock)) {
                        level.removeBlock(clearPos, false);
                    }
                }
            }
        }
    }

    public static boolean placeBed(ServerLevel level, BlockPos footPos, Block bedBlock) {
        // Force-load chunks so beds can be placed at large distances
        level.getChunk(footPos.getX() >> 4, footPos.getZ() >> 4);
        Direction facing = Direction.SOUTH;
        BlockPos headPos = footPos.relative(facing);
        level.getChunk(headPos.getX() >> 4, headPos.getZ() >> 4);

        BlockState footState = bedBlock.defaultBlockState()
                .setValue(BedBlock.FACING, facing)
                .setValue(BedBlock.PART, BedPart.FOOT);
        BlockState headState = bedBlock.defaultBlockState()
                .setValue(BedBlock.FACING, facing)
                .setValue(BedBlock.PART, BedPart.HEAD);

        level.setBlock(footPos, footState, Block.UPDATE_ALL);
        level.setBlock(headPos, headState, Block.UPDATE_ALL);
        return true;
    }
}
