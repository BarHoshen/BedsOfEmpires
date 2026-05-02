package com.bedsofempires.item;

import com.bedsofempires.entity.BedFinderEntity;
import com.bedsofempires.game.BedRegistry;
import com.bedsofempires.game.GameSavedData;
import com.bedsofempires.game.GameState;
import com.bedsofempires.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BedCompassItem extends Item {

    public BedCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Points toward the nearest enemy bed"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }

        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.pass(stack);
        }

        ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return InteractionResultHolder.fail(stack);

        GameSavedData data = GameSavedData.get(overworld);
        if (data.getGameState() != GameState.IN_PROGRESS) {
            serverPlayer.sendSystemMessage(Component.literal("The Bed Compass only works during a game."));
            return InteractionResultHolder.fail(stack);
        }

        BlockPos nearestEnemyBed = findNearestEnemyBed(data.getBedRegistry(), serverPlayer);

        if (nearestEnemyBed == null) {
            serverPlayer.sendSystemMessage(Component.literal("The Bed Compass finds nothing..."));
            return InteractionResultHolder.fail(stack);
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL,
                0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));

        BedFinderEntity entity = new BedFinderEntity(ModEntities.BED_FINDER.get(), level);
        entity.setPos(player.getX(), player.getEyeY() - 0.1, player.getZ());
        entity.setTargetPos(nearestEnemyBed);
        entity.setItem(stack);
        level.addFreshEntity(entity);

        player.awardStat(Stats.ITEM_USED.get(this));
        stack.consume(1, player);

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private BlockPos findNearestEnemyBed(BedRegistry registry, ServerPlayer player) {
        UUID playerId = player.getUUID();
        BlockPos playerPos = player.blockPosition();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Map.Entry<BlockPos, UUID> entry : registry.getAllBeds().entrySet()) {
            if (entry.getValue().equals(playerId)) continue;
            double dist = entry.getKey().distSqr(playerPos);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = entry.getKey();
            }
        }
        return nearest;
    }
}
