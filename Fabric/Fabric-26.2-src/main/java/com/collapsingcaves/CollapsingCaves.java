package com.collapsingcaves;

import com.collapsingcaves.cavein.CaveInBlockRegistry;
import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.command.CaveCooldownCommand;
import com.collapsingcaves.command.TriggerCollapseCommand;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.network.CaveInNetworking;
import com.collapsingcaves.sound.CaveInSounds;
import com.collapsingcaves.tracking.PlacedBlockTracker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollapsingCaves implements ModInitializer {
    public static final String MOD_ID = "collapsingcaves";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("CollapsingCaves initializing...");

        // Load config
        CollapsingCavesConfig.load();

        // Register sounds
        CaveInSounds.register();

        // Register network payloads
        CaveInNetworking.registerPayloads();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TriggerCollapseCommand.register(dispatcher);
            CaveCooldownCommand.register(dispatcher);
        });

        // Resolve block registry when server starts (registries are frozen by then)
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            CaveInBlockRegistry.resolve();
            LOGGER.info("CollapsingCaves block registry resolved.");
        });

        // Clean up on server stop
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            CaveInManager.clearAll();
        });

        // Tick all active cave-ins
        ServerTickEvents.END_LEVEL_TICK.register(world -> {
            if (world instanceof ServerLevel serverLevel) {
                CaveInManager.get(serverLevel).tick();
            }
        });

        // Detect block breaks and trigger cave-ins
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
                // Remove from placed tracker if it was player-placed
                PlacedBlockTracker tracker = PlacedBlockTracker.get(serverLevel);
                if (tracker.isPlayerPlaced(pos)) {
                    tracker.removePlaced(pos);
                    return;
                }

                CaveInManager.get(serverLevel).onBlockBroken(serverPlayer, pos, state);
            }
        });

        LOGGER.info("CollapsingCaves initialized successfully!");
    }
}
