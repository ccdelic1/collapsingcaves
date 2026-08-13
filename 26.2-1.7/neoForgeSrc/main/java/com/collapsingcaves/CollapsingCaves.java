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
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("collapsingcaves")
public class CollapsingCaves {
   public static final String MOD_ID = "collapsingcaves";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath(MOD_ID, path);
   }

   public CollapsingCaves(IEventBus modEventBus, ModContainer modContainer) {
      CollapsingCavesConfig.load();
      CaveInSounds.SOUND_EVENTS.register(modEventBus);
      CaveInNetworking.registerPayloads(modEventBus);
      NeoForge.EVENT_BUS.register(this);
      LOGGER.info("CollapsingCaves is ready.");
   }

   @SubscribeEvent
   public void onRegisterCommands(RegisterCommandsEvent event) {
      TriggerCollapseCommand.register(event.getDispatcher());
      CollapseCooldownCommand.register(event.getDispatcher());
      SetCooldownCommand.register(event.getDispatcher());
      CanCollapseCheckCommand.register(event.getDispatcher());
   }

   @SubscribeEvent
   public void onServerStarted(ServerStartedEvent event) {
      CaveInBlockRegistry.resolve();
   }

   @SubscribeEvent
   public void onServerStopping(ServerStoppingEvent event) {
      CaveInManager.clearAll();
   }

   @SubscribeEvent
   public void onLevelTick(Post event) {
      if (event.getLevel() instanceof ServerLevel serverLevel) {
         CaveInManager.get(serverLevel).tick();
      }
   }

   @SubscribeEvent
   public void onPlayerLogin(PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer serverPlayer) {
         CaveInManager.applyLoginGrace(serverPlayer);
      }
   }
}
