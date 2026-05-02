package com.bedsofempires;

import com.bedsofempires.registry.ModCreativeTabs;
import com.bedsofempires.registry.ModEntities;
import com.bedsofempires.registry.ModItems;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(BedsOfEmpires.MOD_ID)
public class BedsOfEmpires {
    public static final String MOD_ID = "bedsofempires";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BedsOfEmpires(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("Beds of Empires initializing");
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
    }
}
