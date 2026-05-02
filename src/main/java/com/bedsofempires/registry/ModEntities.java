package com.bedsofempires.registry;

import com.bedsofempires.BedsOfEmpires;
import com.bedsofempires.entity.BedFinderEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, BedsOfEmpires.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<BedFinderEntity>> BED_FINDER = ENTITY_TYPES.register("bed_finder",
            () -> EntityType.Builder.<BedFinderEntity>of(BedFinderEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(4)
                    .build("bed_finder"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
