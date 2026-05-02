package com.bedsofempires.registry;

import com.bedsofempires.BedsOfEmpires;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BedsOfEmpires.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AOB_TAB = CREATIVE_TABS.register("aob_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.literal("Beds of Empires"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.BED_COMPASS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BED_COMPASS.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
