package com.collapsingcaves.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

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
    private static CollapsingCavesConfig INSTANCE;

    public double caveInChance = 0.001953125;
    public double caveInSizeMultiplier = 1.0;
    public List<String> blacklistedBlocks = new ArrayList<>();
    public List<String> whitelistedBlocks = new ArrayList<>();
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
        tiers.put("small", new TierConfig(true, 20));
        tiers.put("medium", new TierConfig(true, 20));
        tiers.put("large", new TierConfig(true, 20));
        tiers.put("enormous", new TierConfig(true, 20));
        tiers.put("gargantuan", new TierConfig(true, 20));
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
                INSTANCE = GSON.fromJson(reader, CollapsingCavesConfig.class);
                if (INSTANCE == null) {
                    INSTANCE = new CollapsingCavesConfig();
                }
                if (INSTANCE.tiers == null || INSTANCE.tiers.isEmpty()) {
                    INSTANCE.tiers = new CollapsingCavesConfig().tiers;
                }
                if (INSTANCE.blacklistedBlocks == null) {
                    INSTANCE.blacklistedBlocks = new ArrayList<>();
                }
                if (INSTANCE.whitelistedBlocks == null) {
                    INSTANCE.whitelistedBlocks = new ArrayList<>();
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
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
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
}
