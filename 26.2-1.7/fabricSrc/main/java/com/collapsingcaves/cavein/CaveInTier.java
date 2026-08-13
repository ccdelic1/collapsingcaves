package com.collapsingcaves.cavein;

import com.collapsingcaves.config.CollapsingCavesConfig;
import java.util.Locale;

public enum CaveInTier {
   SMALL("small"),
   MEDIUM("medium"),
   LARGE("large");

   // How far past the edge of the falling area the rumble and the shake still reach, so
   // that neither stops the moment a player steps out of the radius.
   private static final int EFFECT_MARGIN = 10;
   public final String id;

   CaveInTier(String id) {
      this.id = id;
   }

   public String displayName() {
      return this.id.substring(0, 1).toUpperCase(Locale.ROOT) + this.id.substring(1);
   }

   public static CaveInTier byId(String id) {
      for (CaveInTier tier : values()) {
         if (tier.id.equalsIgnoreCase(id)) {
            return tier;
         }
      }

      return null;
   }

   private CollapsingCavesConfig.TierSettings settings() {
      return CollapsingCavesConfig.get().tier(this.id);
   }

   public int weight() {
      return this.settings().weight;
   }

   public int durationTicks() {
      return this.settings().durationSeconds * 20;
   }

   public int clusterIntervalTicks() {
      return this.settings().clusterIntervalSeconds * 20;
   }

   public int minClusterBlocks() {
      return this.settings().minClusterBlocks;
   }

   public int maxClusterBlocks() {
      return this.settings().maxClusterBlocks;
   }

   public int horizontalRadius() {
      return this.settings().radius;
   }

   public int heightAbove() {
      return this.settings().heightAbove;
   }

   public int depthBelow() {
      return this.settings().depthBelow;
   }

   public float shakeIntensity() {
      return (float)this.settings().shakeIntensity;
   }

   // Derived from the radius rather than set on its own, so that a change to the size of
   // a cave-in carries the rumble and the shake with it.
   public int effectRange() {
      return this.horizontalRadius() + EFFECT_MARGIN;
   }
}
