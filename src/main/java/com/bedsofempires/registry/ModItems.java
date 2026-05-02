package com.bedsofempires.registry;

import com.bedsofempires.BedsOfEmpires;
import com.bedsofempires.item.BedCompassItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, BedsOfEmpires.MOD_ID);

    public static final DeferredHolder<Item, BedCompassItem> BED_COMPASS = ITEMS.register("bed_compass",
            () -> new BedCompassItem(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
