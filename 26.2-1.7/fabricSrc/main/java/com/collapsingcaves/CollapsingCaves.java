package com.collapsingcaves;

import com.collapsingcaves.cavein.CaveInBlockRegistry;
import com.collapsingcaves.cavein.CaveInManager;
import com.collapsingcaves.command.CanCollapseCheckCommand;
import com.collapsingcaves.command.CollapseCooldownCommand;
import com.collapsingcaves.command.SetCooldownCommand;
import com.collapsingcaves.command.TriggerCollapseCommand;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.network.CaveInNetworking;
import com.collapsingcaves.sound.CaveInSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.EndLevelTick;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents.After;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Join;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CollapsingCaves implements ModInitializer {
   public static final String MOD_ID = "collapsingcaves";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }

   public void onInitialize() {
      CollapsingCavesConfig.load();
      CaveInSounds.register();
      CaveInNetworking.registerPayloads();
      CommandRegistrationCallback.EVENT.register((CommandRegistrationCallback)(dispatcher, registryAccess, environment) -> {
         TriggerCollapseCommand.register(dispatcher);
         CollapseCooldownCommand.register(dispatcher);
         SetCooldownCommand.register(dispatcher);
         CanCollapseCheckCommand.register(dispatcher);
      });
      ServerLifecycleEvents.SERVER_STARTED.register((ServerStarted)server -> CaveInBlockRegistry.resolve());
      ServerLifecycleEvents.SERVER_STOPPED.register((ServerStopped)server -> CaveInManager.clearAll());
      ServerTickEvents.END_LEVEL_TICK.register((EndLevelTick)serverLevel -> CaveInManager.get(serverLevel).tick());
      ServerPlayConnectionEvents.JOIN.register((Join)(handler, sender, server) -> CaveInManager.applyLoginGrace(handler.player));
      PlayerBlockBreakEvents.AFTER.register((After)(world, player, pos, state, blockEntity) -> {
         if (world instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            CaveInManager.get(serverLevel).onBlockBroken(serverPlayer, pos, state);
         }
      });
      LOGGER.info("CollapsingCaves is ready.");
   }
}
