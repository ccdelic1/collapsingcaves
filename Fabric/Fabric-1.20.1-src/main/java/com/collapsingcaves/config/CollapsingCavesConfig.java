package com.collapsingcaves.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CollapsingCavesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CollapsingCavesConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "collapsingcaves.json";
    private static final String OLD_CHANCE_COMMENT_KEY = "#Chance out of 500 that a cave in happens on block break: [Range 0-500, decimals allowed]";
    private static CollapsingCavesConfig INSTANCE;
    private static final Map<String, TierConfig> DEFAULT_TIERS = createDefaultTiers();
    private static final double MIN_CHANCE_DIVISOR = 1.0;
    private static final double MAX_CHANCE_DIVISOR = 1_000_000.0;
    // Bounds the radius multiplier so a bad/typo'd config value can't blow up the
    // O(radius^3) block scan in CaveInEvent (beginLayerScan/continueScan) into an
    // unreasonably long-running cave-in; the scan itself is time-sliced across ticks
    // (see CaveInEvent#SCAN_POSITIONS_PER_TICK) so an oversized radius no longer risks
    // stalling a single tick, just extends how long the scan takes to finish.
    private static final double MIN_SIZE_MULTIPLIER = 0.1;
    private static final double MAX_SIZE_MULTIPLIER = 5.0;

    public int cooldownMinutes = 20;
    @SerializedName("#Chance of cave in is equal to one divided by the specified caveInChance value.")
    public String caveInChanceComment = "The default of 400 means its a 1 in 400 chance upon breaking an applicable-block that a cave in happens.";
    public double caveInChance = 400.0;
    // Clamped to [MIN_SIZE_MULTIPLIER, MAX_SIZE_MULTIPLIER] on load - see clampSizeMultiplier().
    public double caveInSizeMultiplier = 1.0;
    public List<String> blacklistedBlocks = new ArrayList<>();
    public List<String> whitelistedBlocks = new ArrayList<>();
    @SerializedName("#Weight means the likelihood of this tier cave-in type occurring when a cave-in triggers.")
    public String tiersComment = "Larger cave ins can cause server lag!";
    public Map<String, TierConfig> tiers = new LinkedHashMap<>();
    // Set once a config has passed through the legacy-tier-weight migration so a
    // deliberately-uniform (all weight 20) user config isn't silently reverted on
    // every subsequent load.
    public boolean tierWeightsMigrated = false;

    public static class TierConfig {
        public boolean enabled = true;
        public int weight = 10;

        public TierConfig() {}

        public TierConfig(boolean enabled, int weight) {
            this.enabled = enabled;
            this.weight = weight;
        }
    }

    public CollapsingCavesConfig() {
        tiers.putAll(copyTiers(DEFAULT_TIERS));
    }

    private static Map<String, TierConfig> createDefaultTiers() {
        Map<String, TierConfig> defaults = new LinkedHashMap<>();
        defaults.put("small", new TierConfig(true, 10));
        defaults.put("medium", new TierConfig(true, 37));
        defaults.put("large", new TierConfig(true, 37));
        defaults.put("enormous", new TierConfig(true, 8));
        defaults.put("gargantuan", new TierConfig(true, 8));
        return defaults;
    }

    private static Map<String, TierConfig> copyTiers(Map<String, TierConfig> source) {
        Map<String, TierConfig> copy = new LinkedHashMap<>();
        for (Map.Entry<String, TierConfig> entry : source.entrySet()) {
            TierConfig value = entry.getValue();
            copy.put(entry.getKey(), new TierConfig(value.enabled, value.weight));
        }
        return copy;
    }

    private static boolean isLegacyUniformTierWeights(Map<String, TierConfig> source) {
        if (source == null || source.size() != 5) {
            return false;
        }

        for (String key : DEFAULT_TIERS.keySet()) {
            TierConfig tier = source.get(key);
            if (tier == null || !tier.enabled || tier.weight != 20) {
                return false;
            }
        }

        return true;
    }

    private static void normalizeAndMigrateTierWeights(CollapsingCavesConfig config) {
        Map<String, TierConfig> source = config.tiers;
        // Only auto-correct the old uniform-weight default once. Otherwise a server
        // admin who deliberately sets every tier's weight to 20 would have that
        // choice silently reverted back to the biased defaults on every restart.
        if (!config.tierWeightsMigrated && isLegacyUniformTierWeights(source)) {
            config.tiers = copyTiers(DEFAULT_TIERS);
            config.tierWeightsMigrated = true;
            return;
        }
        config.tierWeightsMigrated = true;

        Map<String, TierConfig> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, TierConfig> entry : DEFAULT_TIERS.entrySet()) {
            String key = entry.getKey();
            TierConfig existing = source != null ? source.get(key) : null;
            if (existing == null) {
                TierConfig fallback = entry.getValue();
                normalized.put(key, new TierConfig(fallback.enabled, fallback.weight));
            } else {
                normalized.put(key, new TierConfig(existing.enabled, existing.weight));
            }
        }
        config.tiers = normalized;
    }

    private static double clampChanceDivisor(double value) {
        return Math.max(MIN_CHANCE_DIVISOR, Math.min(MAX_CHANCE_DIVISOR, value));
    }

    private static double clampSizeMultiplier(double value) {
        if (Double.isNaN(value)) {
            return 1.0;
        }
        return Math.max(MIN_SIZE_MULTIPLIER, Math.min(MAX_SIZE_MULTIPLIER, value));
    }

    private static double convertLegacyProbabilityToDivisor(double legacyProbability) {
        if (legacyProbability <= 0.0) {
            return 400.0;
        }
        return clampChanceDivisor(1.0 / legacyProbability);
    }

    private static void migrateChanceFromJson(CollapsingCavesConfig config, JsonObject json) {
        if (json == null || !json.has("caveInChance") || json.get("caveInChance").isJsonNull()) {
            config.caveInChance = clampChanceDivisor(config.caveInChance);
            return;
        }

        try {
            double raw = json.get("caveInChance").getAsDouble();
            if (json.has(OLD_CHANCE_COMMENT_KEY)) {
                if (raw <= 0.0) {
                    config.caveInChance = 400.0;
                } else {
                    config.caveInChance = clampChanceDivisor(500.0 / raw);
                }
                return;
            }

            if (raw > 0.0 && raw <= 0.01) {
                config.caveInChance = convertLegacyProbabilityToDivisor(raw);
            } else {
                config.caveInChance = clampChanceDivisor(raw);
            }
        } catch (Exception ignored) {
            config.caveInChance = clampChanceDivisor(config.caveInChance);
        }
    }

    public static CollapsingCavesConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                INSTANCE = GSON.fromJson(json, CollapsingCavesConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new CollapsingCavesConfig();
                }
                migrateChanceFromJson(INSTANCE, json);
                if (INSTANCE.tiers == null || INSTANCE.tiers.isEmpty()) {
                    INSTANCE.tiers = copyTiers(DEFAULT_TIERS);
                    INSTANCE.tierWeightsMigrated = true;
                } else {
                    normalizeAndMigrateTierWeights(INSTANCE);
                }
                if (INSTANCE.blacklistedBlocks == null) {
                    INSTANCE.blacklistedBlocks = new ArrayList<>();
                }
                if (INSTANCE.whitelistedBlocks == null) {
                    INSTANCE.whitelistedBlocks = new ArrayList<>();
                }
                if (INSTANCE.cooldownMinutes <= 0) {
                    INSTANCE.cooldownMinutes = 20;
                }
                INSTANCE.caveInSizeMultiplier = clampSizeMultiplier(INSTANCE.caveInSizeMultiplier);
                if (INSTANCE.caveInChanceComment == null || INSTANCE.caveInChanceComment.isBlank()) {
                    INSTANCE.caveInChanceComment = "";
                }
                if (INSTANCE.tiersComment == null || INSTANCE.tiersComment.isBlank()) {
                    INSTANCE.tiersComment = "";
                }
            } catch (Exception e) {
                // Catches IOException as well as Gson's unchecked JsonSyntaxException /
                // IllegalStateException from a malformed or non-object config file, so a
                // corrupt collapsingcaves.json can't take mod init down with it.
                LOGGER.warn("Failed to read {}, regenerating with defaults.", CONFIG_FILE, e);
                INSTANCE = new CollapsingCavesConfig();
                INSTANCE.tierWeightsMigrated = true;
            }
        } else {
            INSTANCE = new CollapsingCavesConfig();
            INSTANCE.tierWeightsMigrated = true;
        }
        save();
    }

    public static void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            LOGGER.error("Failed to write {}.", CONFIG_FILE, e);
        }
    }

    public boolean isTierEnabled(String tierId) {
        TierConfig config = tiers.get(tierId);
        return config != null && config.enabled;
    }

    public int getTierWeight(String tierId) {
        TierConfig config = tiers.get(tierId);
        return config != null ? config.weight : 0;
    }

    public long getCooldownTicks() {
        return Math.max(0, cooldownMinutes) * 1200L;
    }

    public double getCaveInChanceProbability() {
        double chanceDivisor = clampChanceDivisor(caveInChance);
        return 1.0 / chanceDivisor;
    }
}
