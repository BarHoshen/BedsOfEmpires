package com.bedsofempires.event;

import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import com.bedsofempires.game.LobbyBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "bedsofempires")
public class LobbyHandler {

    private static long lastMonitorUpdate = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.LOBBY) return;

        LobbyBuilder.buildIfNeeded(overworld);

        long gameTime = overworld.getGameTime();
        if (gameTime - lastMonitorUpdate >= 20) {
            lastMonitorUpdate = gameTime;
            LobbyBuilder.updateMonitors(event.getServer());
        }

        BlockPos center = LobbyBuilder.getRoomCenter();
        if (center == null) return;

        int radius = data.getSettings().getLobbyRadius();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            double dx = player.getX() - center.getX();
            double dz = player.getZ() - center.getZ();
            double distSq = dx * dx + dz * dz;

            if (distSq > (double) radius * radius) {
                player.teleportTo(overworld,
                        center.getX() + 0.5, center.getY(), center.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                player.displayClientMessage(
                        Component.literal("You cannot leave the lobby area."), true
                );
            }
        }
    }
}
