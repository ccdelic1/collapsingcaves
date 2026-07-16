package com.collapsingcaves;

import com.collapsingcaves.cavein.CaveInBlockRegistry;
import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.command.CaveCooldownCommand;
import com.collapsingcaves.command.TriggerCollapseCommand;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.network.CaveInNetworking;
import com.collapsingcaves.sound.CaveInSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CollapsingCaves.MOD_ID)
public class CollapsingCaves {
    public static final String MOD_ID = "collapsingcaves";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CollapsingCaves() {
        LOGGER.info("CollapsingCaves initializing...");

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Load config
        CollapsingCavesConfig.load();

        // Register sound events via DeferredRegister
        CaveInSounds.SOUND_EVENTS.register(modEventBus);

        // Register the network channel and its messages
        CaveInNetworking.registerPayloads();

        // Register for game events
        MinecraftForge.EVENT_BUS.register(this);

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
    public void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            CaveInManager.get(serverLevel).tick();
        }
    }

}
