package com.collapsingcaves.client;

import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.config.CollapsingCavesConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class ScreenShakeHandler {
   private static final List<ScreenShakeHandler.ShakeSource> SOURCES = new ArrayList<>();
   private static float currentIntensity = 0.0F;
   private static long shakeSeed = 0L;
   private static ClientLevel lastLevel;

   private ScreenShakeHandler() {
   }

   public static void startShake(BlockPos pos, int tierOrdinal) {
      CaveInTier[] tiers = CaveInTier.values();
      if (tierOrdinal >= 0 && tierOrdinal < tiers.length) {
         SOURCES.add(new ScreenShakeHandler.ShakeSource(pos, tiers[tierOrdinal]));
         shakeSeed = System.nanoTime();
      }
   }

   // Returns whether this client was tracking a cave-in at that position. The stop packet
   // is sent to everyone in the dimension, so without this a player who was never close
   // enough to be told a cave-in started would still be told that one had ended.
   public static boolean stopShake(BlockPos pos) {
      Iterator<ScreenShakeHandler.ShakeSource> iterator = SOURCES.iterator();

      while (iterator.hasNext()) {
         if (iterator.next().pos().equals(pos)) {
            iterator.remove();
            return true;
         }
      }

      return false;
   }

   public static void tick() {
      Minecraft client = Minecraft.getInstance();
      // A dimension change swaps the client level without disconnecting, and the server
      // sends the stop packet only to players still in the cave-in's level. Without this
      // the shake source would survive the trip and then be measured against coordinates
      // in the new dimension, so drop the client state whenever the level changes.
      if (client.level != lastLevel) {
         lastLevel = client.level;
         reset();
         CaveInClientNetworking.clearAll();
      }

      // The seed moves on every tick. It used to be set once when the cave-in started, and
      // the camera then repeated the same path inside each tick, which reads as a steady
      // buzz rather than a shake.
      shakeSeed++;

      if (!SOURCES.isEmpty() && CollapsingCavesConfig.get().screenShakeEnabled) {
         if (client.player == null) {
            currentIntensity = 0.0F;
         } else {
            Vec3 playerPos = client.player.position();
            float strongest = 0.0F;

            for (ScreenShakeHandler.ShakeSource source : SOURCES) {
               double distance = playerPos.distanceTo(Vec3.atCenterOf(source.pos()));
               double maxDistance = source.tier().effectRange();
               if (distance <= maxDistance) {
                  float factor = 1.0F - (float)(distance / maxDistance);
                  float intensity = source.tier().shakeIntensity() * factor;
                  if (intensity > strongest) {
                     strongest = intensity;
                  }
               }
            }

            currentIntensity = strongest;
         }
      } else {
         currentIntensity = 0.0F;
      }
   }

   // Motion sickness mode trades the shake for a pair of on-screen titles, so the camera
   // is left alone entirely. Turning screen shake off still silences both.
   public static float getShakeIntensity() {
      return CollapsingCavesConfig.get().motionSicknessMode ? 0.0F : currentIntensity;
   }

   public static long getShakeSeed() {
      return shakeSeed;
   }

   public static void reset() {
      SOURCES.clear();
      currentIntensity = 0.0F;
   }

   private record ShakeSource(BlockPos pos, CaveInTier tier) {

   }
}
