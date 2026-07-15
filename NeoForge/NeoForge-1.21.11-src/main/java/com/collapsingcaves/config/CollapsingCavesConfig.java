package com.collapsingcaves.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import net.neoforged.fml.loading.FMLPaths;

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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "collapsingcaves.json";
    private static final String OLD_CHANCE_COMMENT_KEY = "#Chance out of 500 that a cave in happens on block break: [Range 0-500, decimals allowed]";
    private static CollapsingCavesConfig INSTANCE;
    private static final Map<String, TierConfig> DEFAULT_TIERS = createDefaultTiers();
    private static final double MIN_CHANCE_DIVISOR = 1.0;
    private static final double MAX_CHANCE_DIVISOR = 1_000_000.0;

    public int cooldownMinutes = 20;
    @SerializedName("#Chance of cave in is equal to one divided by the specified caveInChance value.")
    public String caveInChanceComment = "The default of 400 means its a 1 in 400 chance upon breaking an applicable-block that a cave in happens.";
    public double caveInChance = 400.0;
    public double caveInSizeMultiplier = 1.0;
    public List<String> blacklistedBlocks = new ArrayList<>();
    public List<String> whitelistedBlocks = new ArrayList<>();
    @SerializedName("#Weight means the likelihood of this tier cave-in type occurring when a cave-in triggers.")
    public String tiersComment = "Larger cave ins can cause server lag!";
    public Map<String, TierConfig> tiers = new LinkedHashMap<>();

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
        if (isLegacyUniformTierWeights(source)) {
            config.tiers = copyTiers(DEFAULT_TIERS);
            return;
        }

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
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
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
                if (INSTANCE.caveInChanceComment == null || INSTANCE.caveInChanceComment.isBlank()) {
                    INSTANCE.caveInChanceComment = "";
                }
                if (INSTANCE.tiersComment == null || INSTANCE.tiersComment.isBlank()) {
                    INSTANCE.tiersComment = "";
                }
            } catch (IOException e) {
                INSTANCE = new CollapsingCavesConfig();
            }
        } else {
            INSTANCE = new CollapsingCavesConfig();
        }
        save();
    }

    public static void save() {
        Path configPath = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        try (Writer writer = Files.newBufferedWriter(configPath)) {
            GSON.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
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
