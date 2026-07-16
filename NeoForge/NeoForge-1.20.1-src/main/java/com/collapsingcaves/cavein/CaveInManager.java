package com.collapsingcaves.cavein;

import com.collapsingcaves.CollapsingCaves;
import com.collapsingcaves.config.CollapsingCavesConfig;
import com.collapsingcaves.network.CaveInNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class CaveInManager {
    private static final Map<ServerLevel, CaveInManager> MANAGERS = new HashMap<>();
    private static final Map<UUID, CaveInTier> FORCED_CAVE_INS = new HashMap<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static final int MAX_CONCURRENT_CAVE_INS = 3;

    private final ServerLevel level;
    private final List<CaveInEvent> activeCaveIns = new ArrayList<>();
    private final Random random = new Random();

    private CaveInManager(ServerLevel level) {
        this.level = level;
    }

    public static CaveInManager get(ServerLevel level) {
        return MANAGERS.computeIfAbsent(level, CaveInManager::new);
    }

    public static void clearAll() {
        MANAGERS.clear();
        FORCED_CAVE_INS.clear();
        COOLDOWNS.clear();
    }

    public static void setForcedCaveIn(ServerPlayer player, CaveInTier tier) {
        FORCED_CAVE_INS.put(player.getUUID(), tier);
    }

    public static long getCooldownRemaining(ServerPlayer player) {
        Long cooldownEnd = COOLDOWNS.get(player.getUUID());
        if (cooldownEnd == null) return 0;
        long remaining = cooldownEnd - player.level().getGameTime();
        return Math.max(0, remaining);
    }

    public void tick() {
        activeCaveIns.removeIf(event -> {
            if (event.isFinished()) {
                CaveInNetworking.sendCaveInStopToNearbyPlayers(level, event.getCenter(), event.getTier());
                CollapsingCaves.LOGGER.info("[CaveCollapse] {} cave-in event over.", event.getTierName());
                return true;
            }
            return false;
        });
        for (CaveInEvent event : activeCaveIns) {
            event.tick();
        }
    }

    public void onBlockBroken(ServerPlayer player, BlockPos pos, BlockState brokenState) {
        // Player-placed blocks are already filtered out by ServerPlayerGameModeMixin before
        // this is called, so no need to re-check PlacedBlockTracker here.
        if (!CaveInBlockRegistry.isAffected(brokenState)) {
            return;
        }

        // Don't trigger cave-ins for surface blocks (block is already air after break, so canSeeSky works)
        if (level.canSeeSky(pos)) {
            return;
        }

        if (activeCaveIns.size() >= MAX_CONCURRENT_CAVE_INS) {
            return;
        }

        CollapsingCavesConfig config = CollapsingCavesConfig.get();

        // Check for forced cave-in from /triggercollapse command
        CaveInTier forcedTier = FORCED_CAVE_INS.remove(player.getUUID());
        CaveInTier tier;
        boolean isForced = false;

        if (forcedTier != null) {
            tier = forcedTier;
            isForced = true;
        } else {
            // Check cooldown for natural cave-ins
            Long cooldownEnd = COOLDOWNS.get(player.getUUID());
            if (cooldownEnd != null && level.getGameTime() < cooldownEnd) {
                return;
            }

            if (random.nextDouble() >= config.getCaveInChanceProbability()) {
                return;
            }
            tier = selectTier(config);
            if (tier == null) {
                return;
            }
        }

        CaveInEvent event = new CaveInEvent(level, player, tier, config.caveInSizeMultiplier);
        if (event.isFinished()) {
            return;
        }

        activeCaveIns.add(event);
        CaveInNetworking.sendCaveInToNearbyPlayers(level, player.blockPosition(), tier);
        CollapsingCaves.LOGGER.info("[CaveCollapse] {} cave-in event starting.", tier.id);

        // Apply cooldown only for natural (non-forced) cave-ins
        if (!isForced) {
            COOLDOWNS.put(player.getUUID(), level.getGameTime() + config.getCooldownTicks());
        }
    }

    private CaveInTier selectTier(CollapsingCavesConfig config) {
        int totalWeight = 0;
        List<CaveInTier> enabledTiers = new ArrayList<>();
        List<Integer> weights = new ArrayList<>();

        for (CaveInTier tier : CaveInTier.values()) {
            if (config.isTierEnabled(tier.id)) {
                int weight = config.getTierWeight(tier.id);
                if (weight > 0) {
                    enabledTiers.add(tier);
                    weights.add(weight);
                    totalWeight += weight;
                }
            }
        }

        if (enabledTiers.isEmpty() || totalWeight == 0) {
            return null;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < enabledTiers.size(); i++) {
            cumulative += weights.get(i);
            if (roll < cumulative) {
                return enabledTiers.get(i);
            }
        }

        return enabledTiers.get(enabledTiers.size() - 1);
    }
}
