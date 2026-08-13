package com.collapsingcaves.client;

import com.collapsingcaves.config.CollapsingCavesConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CaveInGreeting {
   private static final String MARKER_NAME = "collapsingcaves_greeted";

   private CaveInGreeting() {
   }

   // Shown once per installation rather than once per world. The message is about a client
   // side setting, so a marker beside the client's own config is what matches its scope: a
   // world save would repeat the greeting on every new world, and a server would never show
   // it to somebody joining from a different machine.
   public static void showOnce() {
      Minecraft client = Minecraft.getInstance();
      if (client.player == null) {
         return;
      }

      Path marker = CollapsingCavesConfig.configDirectory().resolve(MARKER_NAME);
      if (Files.exists(marker)) {
         return;
      }

      client.player
         .sendSystemMessage(
            Component.literal("Thanks for downloading ")
               .withStyle(ChatFormatting.WHITE)
               .append(Component.literal("Collapsing Caves").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
               .append(Component.literal(" by ").withStyle(ChatFormatting.WHITE))
               .append(Component.literal("CCDelic!").withStyle(ChatFormatting.YELLOW))
         );
      client.player
         .sendSystemMessage(
            Component.literal("Please toggle ")
               .withStyle(ChatFormatting.WHITE)
               .append(Component.literal("Motion Sickness").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
               .append(Component.literal(" mode in the config if screen shaking is an issue for you.").withStyle(ChatFormatting.WHITE))
         );

      try {
         Files.createDirectories(marker.getParent());
         Files.createFile(marker);
      } catch (IOException e) {
         // If the marker cannot be written the greeting simply appears again next time, which
         // is a far better failure than never greeting at all.
      }
   }
}
