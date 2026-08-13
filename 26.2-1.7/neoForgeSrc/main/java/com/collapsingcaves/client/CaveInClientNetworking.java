package com.collapsingcaves.client;

import com.collapsingcaves.CollapsingCaves;
import com.collapsingcaves.cavein.CaveInTier;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.network.CaveInPayload;
import com.collapsingcaves.network.CaveInStopPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CaveInClientNetworking {
   public static final List<CaveInRumbleInstance> ACTIVE_RUMBLES = new ArrayList<>();
   // Ten ticks fading in, forty held, ten fading out: three seconds on screen in total.
   private static final int TITLE_FADE_IN = 10;
   private static final int TITLE_STAY = 40;
   private static final int TITLE_FADE_OUT = 10;
   private static Object titleOwner;

   private CaveInClientNetworking() {
   }

   public static void handleCaveInStart(CaveInPayload payload) {
      Minecraft.getInstance().execute(() -> {
         CaveInTier[] tiers = CaveInTier.values();
         int ordinal = payload.tierOrdinal();
         if (ordinal >= 0 && ordinal < tiers.length) {
            ScreenShakeHandler.startShake(payload.pos(), ordinal);
            if (CollapsingCavesConfig.get().motionSicknessMode) {
               showTitle(Component.literal("Cave-In Triggered!"));
            }
            if (CollapsingCavesConfig.get().soundsEnabled) {
               float volume = tiers[ordinal].effectRange() / 16.0F;
               CaveInRumbleInstance rumble = new CaveInRumbleInstance(payload.pos(), volume);
               ACTIVE_RUMBLES.add(rumble);
               Minecraft.getInstance().getSoundManager().play(rumble);
            }
         }
      });
   }

   public static void handleCaveInStop(CaveInStopPayload payload) {
      Minecraft.getInstance().execute(() -> {
         boolean wasTracked = ScreenShakeHandler.stopShake(payload.pos());
         if (wasTracked && CollapsingCavesConfig.get().motionSicknessMode) {
            showTitle(Component.literal("Cave-In Over!"));
         }

         Iterator<CaveInRumbleInstance> iterator = ACTIVE_RUMBLES.iterator();

         while (iterator.hasNext()) {
            CaveInRumbleInstance rumble = iterator.next();
            if (rumble.getCenter().equals(payload.pos())) {
               rumble.stopRumble();
               iterator.remove();
               break;
            }
         }
      });
   }

   // 26.2 moved the title methods off Gui and onto a Hud that Gui holds; earlier 26.x
   // keeps them on Gui itself. There is no single call that compiles against both, so the
   // owner is found once at run time. This is safe here only because the 26 series ships
   // unobfuscated and these jars keep their real names, which is not true of 1.20 or 1.21.
   private static Object titleOwner() {
      Object gui = Minecraft.getInstance().gui;
      if (titleOwner == null) {
         try {
            titleOwner = gui.getClass().getField("hud").get(gui);
         } catch (NoSuchFieldException e) {
            titleOwner = gui;
         } catch (ReflectiveOperationException e) {
            titleOwner = gui;
         }
      }

      return titleOwner;
   }

   private static void showTitle(Component text) {
      try {
         Object owner = titleOwner();
         owner.getClass()
            .getMethod("setTimes", Integer.TYPE, Integer.TYPE, Integer.TYPE)
            .invoke(owner, TITLE_FADE_IN, TITLE_STAY, TITLE_FADE_OUT);
         owner.getClass().getMethod("setTitle", Component.class).invoke(owner, text);
      } catch (ReflectiveOperationException e) {
         CollapsingCaves.LOGGER.warn("Could not show the cave-in title on this version.", e);
      }
   }

   public static void clearAll() {
      for (CaveInRumbleInstance rumble : ACTIVE_RUMBLES) {
         rumble.stopRumble();
      }

      ACTIVE_RUMBLES.clear();
   }
}
