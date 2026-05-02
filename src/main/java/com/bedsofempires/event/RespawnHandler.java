package com.bedsofempires.event;

import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = "bedsofempires")
public class RespawnHandler {

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;
        if (!data.getParticipants().contains(player.getUUID())) return;

        if (data.isEliminated(player.getUUID())) {
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            return;
        }

        BlockPos bedPos = data.getBedRegistry().getBedPos(player.getUUID());
        if (bedPos != null) {
            player.teleportTo(overworld,
                    bedPos.getX() + 0.5, bedPos.getY() + 0.5, bedPos.getZ() + 0.5,
                    player.getYRot(), player.getXRot());
        }
    }
}
