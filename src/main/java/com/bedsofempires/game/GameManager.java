package com.bedsofempires.game;

import com.bedsofempires.BedsOfEmpires;
import com.bedsofempires.integration.JourneyMapIntegration;
import com.bedsofempires.integration.ServerRestartHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class GameManager {
    private static boolean bypassBedPlacement = false;
    private static final Map<UUID, String> playerColors = new HashMap<>();
    private static long endGameTick = 0;
    private static boolean pendingRestart = false;

    public static boolean isBypassingBedPlacement() {
        return bypassBedPlacement;
    }

    public static Map<UUID, String> getPlayerColors() {
        return playerColors;
    }

    public static void setPlayerColor(UUID playerId, String color) {
        playerColors.put(playerId, color);
    }

    public static boolean startGame(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return false;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) return false;

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        if (players.isEmpty()) return false;

        List<UUID> playerIds = players.stream()
                .map(p -> p.getUUID())
                .collect(Collectors.toList());

        for (UUID id : playerIds) {
            data.addParticipant(id);
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("Starting match, loading might take a few seconds..."), false
        );

        bypassBedPlacement = true;
        Map<UUID, BlockPos> bedPositions = BedSpawner.spawnBeds(
                overworld, playerIds, playerColors, data.getBedRegistry(), data.getSettings()
        );
        bypassBedPlacement = false;

        data.setGameState(GameState.IN_PROGRESS);
        data.setGameStartTick(overworld.getGameTime());
        data.markDirty();

        // JourneyMap integration - disable player tracking
        Path serverDir = server.getServerDirectory();
        JourneyMapIntegration.disablePlayerTracking(serverDir);

        for (ServerPlayer player : players) {
            BlockPos bedPos = bedPositions.get(player.getUUID());
            if (bedPos != null) {
                // Force-load the chunk so the teleport works at large distances
                int chunkX = bedPos.getX() >> 4;
                int chunkZ = bedPos.getZ() >> 4;
                overworld.getChunk(chunkX, chunkZ);
                player.teleportTo(overworld,
                        bedPos.getX() + 0.5, bedPos.getY(), bedPos.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                player.setGameMode(GameType.SURVIVAL);
            }
        }

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal("Game Starts!")));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal("STONE AGE")));
        }

        overworld.getServer().setDifficulty(data.getSettings().getDifficulty(), true);

        return true;
    }

    public static void resetGame(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);

        for (BlockPos pos : new ArrayList<>(data.getBedRegistry().getAllBedPositions())) {
            overworld.removeBlock(pos, false);
            BlockPos headPos = pos.relative(net.minecraft.core.Direction.SOUTH);
            overworld.removeBlock(headPos, false);
        }

        data.resetAll();
        playerColors.clear();
        pendingRestart = false;

        LobbyBuilder.markNeedsRebuild();

        BlockPos spawn = overworld.getSharedSpawnPos();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.getInventory().clearContent();
            player.setExperienceLevels(0);
            player.setExperiencePoints(0);
            JourneyMapIntegration.clearPlayerData(server.getServerDirectory(), player.getUUID());
            player.setGameMode(GameType.ADVENTURE);
            player.teleportTo(overworld,
                    spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }

        server.getPlayerList().broadcastSystemMessage(
                Component.literal("The game has been reset. Returning to lobby."), false
        );
    }

    public static void eliminatePlayer(ServerPlayer player, MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        data.addEliminatedPlayer(player.getUUID());
        data.recordElimination(player.getUUID(), overworld.getGameTime());

        player.setGameMode(GameType.SPECTATOR);
        player.sendSystemMessage(Component.literal("Your bed was destroyed! You are now a spectator."));

        server.getPlayerList().broadcastSystemMessage(
                Component.literal(player.getGameProfile().getName() + " has been eliminated!"), false
        );

        checkWinCondition(server);
    }

    public static void checkWinCondition(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (!data.getSettings().isAutoEnd()) return;
        if (data.getGameState() != GameState.IN_PROGRESS) return;

        Set<UUID> aliveOwners = data.getBedRegistry().getAliveOwners();

        if (data.getSettings().getTeamMode().equals("allied")) {
            TeamManager tm = data.getTeamManager();
            Set<UUID> aliveTeams = new HashSet<>();
            Set<UUID> soloPlayers = new HashSet<>();
            for (UUID owner : aliveOwners) {
                UUID teamId = tm.getTeam(owner);
                if (teamId != null) {
                    aliveTeams.add(teamId);
                } else {
                    soloPlayers.add(owner);
                }
            }
            int aliveCount = aliveTeams.size() + soloPlayers.size();
            if (aliveCount <= 1) {
                triggerGameEnd(server, aliveOwners);
            }
        } else {
            if (aliveOwners.size() <= 1) {
                triggerGameEnd(server, aliveOwners);
            }
        }
    }

    public static boolean respawnPlayer(ServerPlayer player, MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return false;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getSettings().isAutoEnd()) return false;
        if (!data.isEliminated(player.getUUID())) return false;

        // Cooldown check
        long elapsed = overworld.getGameTime() - data.getEliminationTime(player.getUUID());
        long cooldownTicks = data.getSettings().getRespawnCooldown() * 20L;
        if (elapsed < cooldownTicks) return false;

        // Clear inventory
        player.getInventory().clearContent();

        // Determine bed block color
        bypassBedPlacement = true;
        String colorName = playerColors.get(player.getUUID());
        Block bedBlock;
        if (colorName != null) {
            bedBlock = BedSpawner.getBedByColorName(colorName);
        } else {
            bedBlock = BedSpawner.getRandomBedColor(overworld.getRandom(), new HashSet<>());
        }

        int searchRadius = Math.max(100000, server.getPlayerList().getPlayers().size() * 50000 * data.getSettings().getSearchRadiusScale());
        List<BlockPos> existingBeds = new ArrayList<>(data.getBedRegistry().getAllBedPositions());
        BlockPos newBedPos = null;

        for (int attempt = 0; attempt < 500; attempt++) {
            int x = overworld.getRandom().nextInt(searchRadius * 2) - searchRadius;
            int z = overworld.getRandom().nextInt(searchRadius * 2) - searchRadius;
            BlockPos candidate = findSurface(overworld, x, z);
            if (candidate == null) continue;
            BlockPos headPos = candidate.relative(Direction.SOUTH);
            BlockPos headSurface = findSurface(overworld, headPos.getX(), headPos.getZ());
            if (headSurface == null || headSurface.getY() != candidate.getY()) continue;
            if (!isFarEnough(candidate, existingBeds, data.getSettings().getBedDistance())) continue;
            newBedPos = candidate;
            break;
        }

        if (newBedPos == null) {
            bypassBedPlacement = false;
            return false;
        }

        BedSpawner.placeBed(overworld, newBedPos, bedBlock);
        bypassBedPlacement = false;

        data.getBedRegistry().registerBed(newBedPos, player.getUUID());
        data.getEliminatedPlayers().remove(player.getUUID());
        data.markDirty();

        JourneyMapIntegration.clearPlayerData(server.getServerDirectory(), player.getUUID());

        player.teleportTo(overworld,
                newBedPos.getX() + 0.5, newBedPos.getY(), newBedPos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
        player.setGameMode(GameType.SURVIVAL);

        return true;
    }

    private static BlockPos findSurface(ServerLevel level, int x, int z) {
        BlockPos surfacePos = level.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                new BlockPos(x, 0, z));
        BlockPos below = surfacePos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) return null;
        if (level.getBlockState(surfacePos).isAir() && level.getBlockState(surfacePos.above()).isAir()) {
            return surfacePos;
        }
        return null;
    }

    private static boolean isFarEnough(BlockPos pos, List<BlockPos> existing, int minDistance) {
        for (BlockPos other : existing) {
            if (Math.sqrt(pos.distSqr(other)) < minDistance) return false;
        }
        return true;
    }

    private static void triggerGameEnd(MinecraftServer server, Set<UUID> winners) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        data.setGameState(GameState.ENDED);

        if (winners.isEmpty()) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("The game has ended! No winner."), false
            );
        } else {
            UUID winnerId = winners.iterator().next();
            ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
            String winnerName = winner != null ? winner.getGameProfile().getName() : "Unknown";
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal(winnerName + " has won the game!"), false
            );
        }

        // Schedule world reset and server restart after 30 seconds (600 ticks)
        pendingRestart = true;
        endGameTick = overworld.getGameTime();
        BedsOfEmpires.LOGGER.info("Game ended. World reset and server restart scheduled in 30 seconds.");
    }

    public static void tickEndGame(MinecraftServer server) {
        if (!pendingRestart) return;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;
        long elapsed = overworld.getGameTime() - endGameTick;
        if (elapsed >= 600) { // 30 seconds = 600 ticks
            pendingRestart = false;
            Path serverDir = server.getServerDirectory();
            ServerRestartHelper.prepareWorldReset(serverDir);
            ServerRestartHelper.triggerRestart(server);
        }
    }

    public static void tick(MinecraftServer server) {
        tickEndGame(server);
    }
}
