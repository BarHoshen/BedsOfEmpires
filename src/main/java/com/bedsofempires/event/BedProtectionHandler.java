package com.bedsofempires.event;

import com.bedsofempires.game.BedRegistry;
import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.UUID;

@EventBusSubscriber(modid = "bedsofempires")
public class BedProtectionHandler {

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;

        BlockPos pos = event.getPos();
        int radius = data.getSettings().getProtectionRadius();

        if (data.getBedRegistry().isWithinProtectionRadius(pos, radius)) {
            event.setCanceled(true);
            Entity entity = event.getEntity();
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(
                        Component.literal("You cannot build near a game bed."), true
                );
                player.inventoryMenu.sendAllDataToRemote();
            }
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;

        BlockPos pos = event.getPos();
        int radius = data.getSettings().getProtectionRadius();

        if (data.getBedRegistry().isWithinProtectionRadius(pos, radius)) {
            // Allow breaking enemy beds (not your own or allied)
            if (isEnemyBed(level, pos, event.getPlayer(), data)) {
                return;
            }
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer player) {
                player.displayClientMessage(
                        Component.literal("You cannot break blocks near a game bed."), true
                );
            }
        }
    }

    private static boolean isEnemyBed(ServerLevel level, BlockPos pos, net.minecraft.world.entity.player.Player breaker, GameSavedData data) {
        if (!(level.getBlockState(pos).getBlock() instanceof BedBlock)) return false;

        BlockPos footPos = pos;
        if (level.getBlockState(pos).getValue(BedBlock.PART) == BedPart.HEAD) {
            footPos = pos.relative(level.getBlockState(pos).getValue(BedBlock.FACING).getOpposite());
        }

        BedRegistry registry = data.getBedRegistry();
        if (!registry.isGameBed(footPos)) return false;

        UUID bedOwner = registry.getOwner(footPos);
        UUID breakerId = breaker.getUUID();

        if (bedOwner.equals(breakerId)) return false;
        if (data.getSettings().getTeamMode().equals("allied")) {
            if (data.getTeamManager().isOnSameTeam(bedOwner, breakerId)) return false;
        }
        return true;
    }
}
