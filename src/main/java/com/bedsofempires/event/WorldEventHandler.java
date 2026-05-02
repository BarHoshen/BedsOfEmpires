package com.bedsofempires.event;

import com.bedsofempires.game.WorldEventManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = "bedsofempires")
public class WorldEventHandler {

    private static final ResourceLocation BLOOD_MOON_HEALTH = ResourceLocation.fromNamespaceAndPath("bedsofempires", "blood_moon_health");
    private static final ResourceLocation BLOOD_MOON_DAMAGE = ResourceLocation.fromNamespaceAndPath("bedsofempires", "blood_moon_damage");

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WorldEventManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!WorldEventManager.isBloodMoonActive()) return;
        if (event.getLevel().isClientSide()) return;

        if (event.getEntity() instanceof Monster monster) {
            if (monster.getAttribute(Attributes.MAX_HEALTH) != null &&
                    !monster.getAttribute(Attributes.MAX_HEALTH).hasModifier(BLOOD_MOON_HEALTH)) {
                monster.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(
                        new AttributeModifier(BLOOD_MOON_HEALTH, 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                );
                monster.setHealth(monster.getMaxHealth());
            }
            if (monster.getAttribute(Attributes.ATTACK_DAMAGE) != null &&
                    !monster.getAttribute(Attributes.ATTACK_DAMAGE).hasModifier(BLOOD_MOON_DAMAGE)) {
                monster.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(
                        new AttributeModifier(BLOOD_MOON_DAMAGE, 0.5, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                );
            }
        }
    }
}
