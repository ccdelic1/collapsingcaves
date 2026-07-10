package com.collapsingcaves;

import com.collapsingcaves.cavein.CaveInBlockRegistry;
import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.command.CaveCooldownCommand;
import com.collapsingcaves.command.TriggerCollapseCommand;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.network.CaveInNetworking;
import com.collapsingcaves.sound.CaveInSounds;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CollapsingCaves.MOD_ID)
public class CollapsingCaves {
    public static final String MOD_ID = "collapsingcaves";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CollapsingCaves(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("CollapsingCaves initializing...");

        // Load config
        CollapsingCavesConfig.load();

        // Register sound events via DeferredRegister
        CaveInSounds.SOUND_EVENTS.register(modEventBus);

        // Register network payloads on the mod event bus
        CaveInNetworking.registerPayloads(modEventBus);

        // Register for game events
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("CollapsingCaves initialized successfully!");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        TriggerCollapseCommand.register(event.getDispatcher());
        CaveCooldownCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        CaveInBlockRegistry.resolve();
        LOGGER.info("CollapsingCaves block registry resolved.");
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        CaveInManager.clearAll();
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            CaveInManager.get(serverLevel).tick();
        }
    }

}