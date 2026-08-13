package com.collapsingcaves.config;

import com.collapsingcaves.cavein.CaveInManager;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

public final class CollapsingCavesConfig {
   private static final String FILE_NAME = "collapsingcaves.conf";
   private static final String[] TIER_IDS = new String[]{"small", "medium", "large"};
   private static CollapsingCavesConfig instance;
   public boolean motionSicknessMode = false;
   public int caveInChance = 512;
   public int maxConcurrentCaveIns = 3;
   public int maxFallingBlocks = 600;
   public int loginGraceMinutes = 20;
   public int caveInCooldownMinutes = 20;
   public int warningSeconds = 3;
   public int scanPositionsPerTick = 16384;
   public double fallDamagePerBlock = 2.0;
   public double fallingBlockSpeed = 0.4;
   public int maxFallDamage = 6;
   public boolean gentleEnchantmentStopsCaveIns = true;
   public boolean seaLevelLimitEnabled = true;
   public boolean screenShakeEnabled = true;
   public boolean soundsEnabled = true;
   public boolean dustParticlesEnabled = true;
   public List<String> blacklistedBlocks = new ArrayList<>();
   public List<String> whitelistedBlocks = new ArrayList<>();
   private final Map<String, CollapsingCavesConfig.TierSettings> tiers = new HashMap<>();

   private CollapsingCavesConfig() {
      this.tiers.put("small", new CollapsingCavesConfig.TierSettings(60, 60, 1, 14, 17, 16, 24, 4, 0.2));
      this.tiers.put("medium", new CollapsingCavesConfig.TierSettings(30, 90, 1, 18, 21, 24, 32, 6, 0.3));
      this.tiers.put("large", new CollapsingCavesConfig.TierSettings(10, 120, 1, 22, 35, 32, 48, 8, 0.4));
   }

   public static CollapsingCavesConfig get() {
      if (instance == null) {
         load();
      }

      return instance;
   }

   public CollapsingCavesConfig.TierSettings tier(String tierId) {
      return this.tiers.get(tierId);
   }

   public long cooldownTicks() {
      return clamp(this.caveInCooldownMinutes, 0, 1440) * 1200L;
   }

   public int warningTicks() {
      return clamp(this.warningSeconds, 0, 60) * 20;
   }

   public long loginGraceTicks() {
      return clamp(this.loginGraceMinutes, 0, 1440) * 1200L;
   }

   public double caveInProbability() {
      return 1.0 / clamp(this.caveInChance, 1, 1000000);
   }

   public static Path configDirectory() {
      return FabricLoader.getInstance().getConfigDir();
   }

   public static void load() {
      CollapsingCavesConfig config = new CollapsingCavesConfig();
      Path path = configDirectory().resolve(FILE_NAME);
      if (Files.exists(path)) {
         try {
            config.readFrom(Files.readAllLines(path, StandardCharsets.UTF_8));
         } catch (IOException e) {
            config = new CollapsingCavesConfig();
         }
      }

      config.applyLimits();
      instance = config;
      save();
   }

   public static void save() {
      if (instance != null) {
         Path path = configDirectory().resolve(FILE_NAME);

         try {
            Files.createDirectories(path.getParent());
            Files.write(path, instance.toLines(), StandardCharsets.UTF_8);
         } catch (IOException var2) {
         }
      }
   }

   private void readFrom(List<String> lines) {
      Map<String, String> values = new HashMap<>();

      for (String line : lines) {
         String text = line.trim();
         if (!text.isEmpty() && !text.startsWith("#")) {
            int separator = text.indexOf(61);
            if (separator > 0) {
               values.put(text.substring(0, separator).trim(), text.substring(separator + 1).trim());
            }
         }
      }

      this.motionSicknessMode = readBoolean(values, "MotionSicknessMode", this.motionSicknessMode);
      this.caveInChance = readInt(values, "caveInChance", this.caveInChance);
      this.maxConcurrentCaveIns = readInt(values, "maxConcurrentCaveIns", this.maxConcurrentCaveIns);
      this.maxFallingBlocks = readInt(values, "maxFallingBlocks", this.maxFallingBlocks);
      this.loginGraceMinutes = readInt(values, "loginGraceMinutes", this.loginGraceMinutes);
      this.caveInCooldownMinutes = readInt(values, "caveInCooldownMinutes", this.caveInCooldownMinutes);
      this.warningSeconds = readInt(values, "warningSeconds", this.warningSeconds);
      this.scanPositionsPerTick = readInt(values, "scanPositionsPerTick", this.scanPositionsPerTick);
      this.fallDamagePerBlock = readDouble(values, "fallDamagePerBlock", this.fallDamagePerBlock);
      this.fallingBlockSpeed = readDouble(values, "fallingBlockSpeed", this.fallingBlockSpeed);
      this.maxFallDamage = readInt(values, "maxFallDamage", this.maxFallDamage);
      this.gentleEnchantmentStopsCaveIns = readBoolean(values, "gentleEnchantmentStopsCaveIns", this.gentleEnchantmentStopsCaveIns);
      this.seaLevelLimitEnabled = readBoolean(values, "seaLevelLimitEnabled", this.seaLevelLimitEnabled);
      this.screenShakeEnabled = readBoolean(values, "screenShakeEnabled", this.screenShakeEnabled);
      this.soundsEnabled = readBoolean(values, "soundsEnabled", this.soundsEnabled);
      this.dustParticlesEnabled = readBoolean(values, "dustParticlesEnabled", this.dustParticlesEnabled);
      this.blacklistedBlocks = readList(values, "blacklistedBlocks");
      this.whitelistedBlocks = readList(values, "whitelistedBlocks");

      for (String tierId : TIER_IDS) {
         CollapsingCavesConfig.TierSettings tier = this.tiers.get(tierId);
         tier.weight = readInt(values, tierId + "Weight", tier.weight);
         tier.durationSeconds = readInt(values, tierId + "DurationSeconds", tier.durationSeconds);
         tier.clusterIntervalSeconds = readInt(values, tierId + "ClusterIntervalSeconds", tier.clusterIntervalSeconds);
         tier.minClusterBlocks = readInt(values, tierId + "MinClusterBlocks", tier.minClusterBlocks);
         tier.maxClusterBlocks = readInt(values, tierId + "MaxClusterBlocks", tier.maxClusterBlocks);
         tier.radius = readInt(values, tierId + "Radius", tier.radius);
         tier.heightAbove = readInt(values, tierId + "HeightAbove", tier.heightAbove);
         tier.depthBelow = readInt(values, tierId + "DepthBelow", tier.depthBelow);
         tier.shakeIntensity = readDouble(values, tierId + "ShakeIntensity", tier.shakeIntensity);
      }
   }

   private void applyLimits() {
      this.caveInChance = clamp(this.caveInChance, 1, 1000000);
      this.maxConcurrentCaveIns = clamp(this.maxConcurrentCaveIns, 1, 16);
      this.maxFallingBlocks = clamp(this.maxFallingBlocks, 10, 4000);
      this.loginGraceMinutes = clamp(this.loginGraceMinutes, 0, 1440);
      this.caveInCooldownMinutes = clamp(this.caveInCooldownMinutes, 0, 1440);
      this.warningSeconds = clamp(this.warningSeconds, 0, 60);
      this.scanPositionsPerTick = clamp(this.scanPositionsPerTick, 1024, 262144);
      this.fallDamagePerBlock = clamp(this.fallDamagePerBlock, 0.0, 100.0);
      this.fallingBlockSpeed = clamp(this.fallingBlockSpeed, 0.04, 1.0);
      this.maxFallDamage = clamp(this.maxFallDamage, 0, 1000);

      for (String tierId : TIER_IDS) {
         CollapsingCavesConfig.TierSettings tier = this.tiers.get(tierId);
         tier.weight = clamp(tier.weight, 0, 1000);
         tier.durationSeconds = clamp(tier.durationSeconds, 5, 3600);
         tier.clusterIntervalSeconds = clamp(tier.clusterIntervalSeconds, 1, 600);
         tier.minClusterBlocks = clamp(tier.minClusterBlocks, 1, 512);
         tier.maxClusterBlocks = clamp(tier.maxClusterBlocks, tier.minClusterBlocks, 512);
         tier.radius = clamp(tier.radius, 2, 64);
         tier.heightAbove = clamp(tier.heightAbove, 2, 128);
         tier.depthBelow = clamp(tier.depthBelow, 0, 64);
         tier.shakeIntensity = clamp(tier.shakeIntensity, 0.0, 1.0);
      }
   }

   private List<String> toLines() {
      List<String> lines = new ArrayList<>();
      lines.add("# CollapsingCaves settings.");
      lines.add("# Delete a line to get the default value again.");
      lines.add("");
      addOption(
         lines,
         "Set to true to replace the cave-in screen shake with a message across the middle of the screen on this client. The setting belongs to whoever is playing, not to the world, so it is read from this file on each side separately.",
         "true or false",
         "MotionSicknessMode",
         String.valueOf(this.motionSicknessMode)
      );
      addOption(
         lines,
         "Chance denominator for a cave-in when a player breaks an applicable block. A value of 512 gives a chance of 1 in 512.",
         "1 - 1000000",
         "caveInChance",
         String.valueOf(this.caveInChance)
      );
      addOption(
         lines, "Maximum number of cave-ins that can occur at the same time.", "1 - 16", "maxConcurrentCaveIns", String.valueOf(this.maxConcurrentCaveIns)
      );
      addOption(
         lines,
         "Maximum number of blocks that can fall at the same time. A low value prevents lag.",
         "10 - 4000",
         "maxFallingBlocks",
         String.valueOf(this.maxFallingBlocks)
      );
      addOption(
         lines,
         "Number of minutes after a player logs in during which no cave-in can occur to that player.",
         "0 - 1440",
         "loginGraceMinutes",
         String.valueOf(this.loginGraceMinutes)
      );
      addOption(
         lines,
         "Number of minutes after a cave-in during which no new cave-in can occur to that player.",
         "0 - 1440",
         "caveInCooldownMinutes",
         String.valueOf(this.caveInCooldownMinutes)
      );
      addOption(
         lines,
         "Number of seconds at the start of a cave-in during which the ground rumbles and the screen shakes but no block falls yet. This time is part of the length of the cave-in, not added to it.",
         "0 - 60",
         "warningSeconds",
         String.valueOf(this.warningSeconds)
      );
      addOption(
         lines,
         "Number of open positions that the mod examines in each tick when a cave-in starts. A high value makes the first blocks fall sooner, but gives more work to the server.",
         "1024 - 262144",
         "scanPositionsPerTick",
         String.valueOf(this.scanPositionsPerTick)
      );
      addOption(
         lines,
         "Damage that a falling block causes for each block of the fall distance. The value of maxFallDamage is the limit.",
         "0.0 - 100.0",
         "fallDamagePerBlock",
         String.valueOf(this.fallDamagePerBlock)
      );
      addOption(
         lines,
         "Maximum damage that one falling block can cause. Two points of damage are equal to one heart. If a block covers a player, suffocation causes more damage.",
         "0 - 1000",
         "maxFallDamage",
         String.valueOf(this.maxFallDamage)
      );
      addOption(
         lines,
         "Speed of a block that falls in a cave-in, against the usual speed. A value of 0.5 gives one half of the usual speed.",
         "0.04 - 1.0",
         "fallingBlockSpeed",
         String.valueOf(this.fallingBlockSpeed)
      );
      addOption(
         lines,
         "Set to true to let the Gentle enchantment prevent cave-ins.",
         "true or false",
         "gentleEnchantmentStopsCaveIns",
         String.valueOf(this.gentleEnchantmentStopsCaveIns)
      );
      addOption(
         lines,
         "Set to true to permit a cave-in only for a player at or below the sea level of the world plus a margin of "
            + CaveInManager.SEA_LEVEL_MARGIN
            + " blocks. The sea level is read from the world generation, so a data pack or a mod that moves it moves this limit as well.",
         "true or false",
         "seaLevelLimitEnabled",
         String.valueOf(this.seaLevelLimitEnabled)
      );
      addOption(lines, "Set to true to shake the screen during a cave-in.", "true or false", "screenShakeEnabled", String.valueOf(this.screenShakeEnabled));
      addOption(lines, "Set to true to play the rumble sound and the block impact sound.", "true or false", "soundsEnabled", String.valueOf(this.soundsEnabled));
      addOption(
         lines,
         "Set to true to show dust particles when a cluster hits the ground.",
         "true or false",
         "dustParticlesEnabled",
         String.valueOf(this.dustParticlesEnabled)
      );

      for (String tierId : TIER_IDS) {
         CollapsingCavesConfig.TierSettings tier = this.tiers.get(tierId);
         String name = tierId.substring(0, 1).toUpperCase(Locale.ROOT) + tierId.substring(1);
         addOption(lines, "Probability weight of a " + tierId + " cave-in against the other sizes.", "0 - 1000", tierId + "Weight", String.valueOf(tier.weight));
         addOption(
            lines, "Number of seconds that a " + tierId + " cave-in continues.", "5 - 3600", tierId + "DurationSeconds", String.valueOf(tier.durationSeconds)
         );
         addOption(
            lines,
            "Number of seconds between two clusters of a " + tierId + " cave-in.",
            "1 - 600",
            tierId + "ClusterIntervalSeconds",
            String.valueOf(tier.clusterIntervalSeconds)
         );
         addOption(
            lines,
            "Minimum number of blocks that fall together in a " + tierId + " cave-in.",
            "1 - 512",
            tierId + "MinClusterBlocks",
            String.valueOf(tier.minClusterBlocks)
         );
         addOption(
            lines,
            "Maximum number of blocks that fall together in a " + tierId + " cave-in.",
            "1 - 512",
            tierId + "MaxClusterBlocks",
            String.valueOf(tier.maxClusterBlocks)
         );
         addOption(
            lines, "Horizontal distance in blocks from the epicentre of a " + tierId + " cave-in.", "2 - 64", tierId + "Radius", String.valueOf(tier.radius)
         );
         addOption(
            lines,
            "Distance in blocks above the epicentre that a " + tierId + " cave-in can reach. A large value lets the mod find the ceiling of a high cave.",
            "2 - 128",
            tierId + "HeightAbove",
            String.valueOf(tier.heightAbove)
         );
         addOption(
            lines,
            "Distance in blocks below the epicentre that a " + tierId + " cave-in can reach.",
            "0 - 64",
            tierId + "DepthBelow",
            String.valueOf(tier.depthBelow)
         );
         addOption(lines, name + " cave-in screen shake strength.", "0.0 - 1.0", tierId + "ShakeIntensity", String.valueOf(tier.shakeIntensity));
      }

      addOption(
         lines,
         "Blocks that must never fall. Write the block ids with a comma between them.",
         "block ids, or empty",
         "blacklistedBlocks",
         String.join(", ", this.blacklistedBlocks)
      );
      addOption(
         lines,
         "More blocks that can fall. Write the block ids with a comma between them.",
         "block ids, or empty",
         "whitelistedBlocks",
         String.join(", ", this.whitelistedBlocks)
      );
      return lines;
   }

   private static void addOption(List<String> lines, String description, String range, String key, String value) {
      lines.add("# " + description);
      lines.add("# Permitted values: " + range);
      lines.add(key + " = " + value);
      lines.add("");
   }

   private static int readInt(Map<String, String> values, String key, int fallback) {
      String raw = values.get(key);
      if (raw == null) {
         return fallback;
      }

      try {
         return Integer.parseInt(raw);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   private static double readDouble(Map<String, String> values, String key, double fallback) {
      String raw = values.get(key);
      if (raw == null) {
         return fallback;
      }

      try {
         double parsed = Double.parseDouble(raw);
         return Double.isNaN(parsed) ? fallback : parsed;
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   private static boolean readBoolean(Map<String, String> values, String key, boolean fallback) {
      String raw = values.get(key);
      return raw == null ? fallback : Boolean.parseBoolean(raw.trim());
   }

   private static List<String> readList(Map<String, String> values, String key) {
      List<String> result = new ArrayList<>();
      String raw = values.get(key);
      if (raw == null) {
         return result;
      }

      for (String part : raw.split(",")) {
         String id = part.trim();
         if (!id.isEmpty()) {
            result.add(id);
         }
      }

      return result;
   }

   private static int clamp(int value, int min, int max) {
      return Math.max(min, Math.min(max, value));
   }

   private static double clamp(double value, double min, double max) {
      return Double.isNaN(value) ? min : Math.max(min, Math.min(max, value));
   }

   public static final class TierSettings {
      public int weight;
      public int durationSeconds;
      public int clusterIntervalSeconds;
      public int minClusterBlocks;
      public int maxClusterBlocks;
      public int radius;
      public int heightAbove;
      public int depthBelow;
      public double shakeIntensity;

      TierSettings(
         int weight,
         int durationSeconds,
         int clusterIntervalSeconds,
         int minClusterBlocks,
         int maxClusterBlocks,
         int radius,
         int heightAbove,
         int depthBelow,
         double shakeIntensity
      ) {
         this.weight = weight;
         this.durationSeconds = durationSeconds;
         this.clusterIntervalSeconds = clusterIntervalSeconds;
         this.minClusterBlocks = minClusterBlocks;
         this.maxClusterBlocks = maxClusterBlocks;
         this.radius = radius;
         this.heightAbove = heightAbove;
         this.depthBelow = depthBelow;
         this.shakeIntensity = shakeIntensity;
      }
   }
}
