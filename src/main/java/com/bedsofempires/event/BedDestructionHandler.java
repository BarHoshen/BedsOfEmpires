package com.bedsofempires.event;

import com.bedsofempires.game.BedRegistry;
import com.bedsofempires.game.GameManager;
import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = "bedsofempires")
public class BedDestructionHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;

        BlockPos footPos = resolveFootPos(level, event.getPos());
        BedRegistry registry = data.getBedRegistry();

        if (registry.isGameBed(footPos) && event.getPlayer() instanceof ServerPlayer player) {
            UUID bedOwner = registry.getOwner(footPos);
            UUID breakerId = player.getUUID();

            if (isOwnOrAlliedBed(data, bedOwner, breakerId)) {
                event.setCanceled(true);
                player.displayClientMessage(
                        Component.literal("You cannot destroy your own bed!"), true
                );
                return;
            }
        }

        handleBedRemoval(level, event.getPos());
    }

    private static boolean isOwnOrAlliedBed(GameSavedData data, UUID bedOwner, UUID breaker) {
        if (bedOwner.equals(breaker)) return true;
        if (data.getSettings().getTeamMode().equals("allied")) {
            return data.getTeamManager().isOnSameTeam(bedOwner, breaker);
        }
        return false;
    }

    private static BlockPos resolveFootPos(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof BedBlock) {
            if (level.getBlockState(pos).getValue(BedBlock.PART) == BedPart.HEAD) {
                return pos.relative(level.getBlockState(pos).getValue(BedBlock.FACING).getOpposite());
            }
        }
        return pos;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        // Always tick game manager (handles pending restart regardless of game state)
        GameManager.tick(server);

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;

        BedRegistry registry = data.getBedRegistry();
        List<BlockPos> destroyed = new ArrayList<>();

        for (BlockPos pos : registry.getAllBedPositions()) {
            if (!(overworld.getBlockState(pos).getBlock() instanceof BedBlock)) {
                destroyed.add(pos);
            }
        }

        for (BlockPos pos : destroyed) {
            UUID owner = registry.getOwner(pos);
            registry.removeBed(pos);
            data.markDirty();

            if (owner != null) {
                broadcastBedDestruction(server, owner);
                GameManager.checkWinCondition(server);
            }
        }
    }

    private static void handleBedRemoval(ServerLevel level, BlockPos pos) {
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;

        BlockPos footPos = resolveFootPos(level, pos);
        BedRegistry registry = data.getBedRegistry();
        if (registry.isGameBed(footPos)) {
            UUID owner = registry.getOwner(footPos);
            registry.removeBed(footPos);
            data.markDirty();

            if (owner != null) {
                broadcastBedDestruction(level.getServer(), owner);
                GameManager.checkWinCondition(level.getServer());
            }
        }
    }

    private static void broadcastBedDestruction(MinecraftServer server, UUID owner) {
        ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(owner);
        String ownerName = ownerPlayer != null ? ownerPlayer.getGameProfile().getName() : "Unknown";

        Component title = Component.literal("☠ BED DESTROYED ☠");
        Component subtitle = Component.literal(ownerName + "'s bed has been destroyed!");

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));

            player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.MASTER, 1.0F, 1.0F);
        }
    }
}
