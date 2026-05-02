package com.bedsofempires.event;

import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import com.bedsofempires.game.LobbyBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "bedsofempires")
public class PlayerJoinHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);

        if (data.getGameState() == GameState.LOBBY) {
            BlockPos center = LobbyBuilder.getRoomCenter();
            if (center != null) {
                player.setRespawnPosition(overworld.dimension(), center, 0.0F, true, false);
                player.teleportTo(overworld,
                        center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
            }
            player.setGameMode(GameType.ADVENTURE);
            return;
        }

        if (data.getGameState() != GameState.IN_PROGRESS) return;

        if (!data.getParticipants().contains(player.getUUID())) {
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal(
                    "A game is in progress. You are spectating."
            ));
            return;
        }

        if (data.isEliminated(player.getUUID())) {
            player.setGameMode(GameType.SPECTATOR);
            player.sendSystemMessage(Component.literal(
                    "Your bed was destroyed while you were away. You are a spectator."
            ));
            return;
        }

        if (!data.getBedRegistry().hasAliveBed(player.getUUID())) {
            player.setGameMode(GameType.SPECTATOR);
            data.addEliminatedPlayer(player.getUUID());
            player.sendSystemMessage(Component.literal(
                    "Your bed was destroyed while you were away. You are now a spectator."
            ));
            return;
        }

        player.setGameMode(GameType.SURVIVAL);
    }
}
