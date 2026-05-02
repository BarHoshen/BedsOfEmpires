package com.bedsofempires.event;

import com.bedsofempires.game.GameManager;
import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = "bedsofempires")
public class DeathHandler {

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;
        if (!data.getParticipants().contains(player.getUUID())) return;
        if (data.isEliminated(player.getUUID())) return;

        if (!data.getBedRegistry().hasAliveBed(player.getUUID())) {
            GameManager.eliminatePlayer(player, player.getServer());
        }
    }
}
