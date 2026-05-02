package com.bedsofempires.event;

import com.bedsofempires.game.GameManager;
import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = "bedsofempires")
public class BedPlacementBlocker {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (GameManager.isBypassingBedPlacement()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) return;

        if (event.getPlacedBlock().getBlock() instanceof BedBlock) {
            event.setCanceled(true);
            Entity entity = event.getEntity();
            if (entity instanceof ServerPlayer player) {
                player.displayClientMessage(
                        Component.literal("You cannot place beds during the game."), true
                );
                player.inventoryMenu.sendAllDataToRemote();
            }
        }
    }
}
