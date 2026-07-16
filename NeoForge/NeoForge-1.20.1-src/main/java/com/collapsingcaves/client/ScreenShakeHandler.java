package com.collapsingcaves.client;

import com.collapsingcaves.cavein.CaveInTier;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ScreenShakeHandler {
    private static final List<ShakeSource> activeSources = new ArrayList<>();
    private static float currentIntensity = 0f;
    private static long shakeSeed = 0;

    private record ShakeSource(BlockPos pos, CaveInTier tier) {}

    public static void startShake(BlockPos pos, int tierOrdinal) {
        CaveInTier tier;
        try {
            tier = CaveInTier.values()[tierOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            return;
        }
        activeSources.add(new ShakeSource(pos, tier));
        shakeSeed = System.nanoTime();
    }

    public static void stopShake(BlockPos pos) {
        Iterator<ShakeSource> it = activeSources.iterator();
        while (it.hasNext()) {
            if (it.next().pos().equals(pos)) {
                it.remove();
                break;
            }
        }
    }

    public static void tick() {
        if (activeSources.isEmpty()) {
            currentIntensity = 0f;
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            currentIntensity = 0f;
            return;
        }

        Vec3 playerPos = mc.player.position();
        float maxIntensity = 0f;

        for (ShakeSource source : activeSources) {
            double dist = playerPos.distanceTo(Vec3.atCenterOf(source.pos()));
            double maxDist = source.tier().shakeMaxDistance;
            if (dist <= maxDist) {
                float distanceFactor = 1.0f - (float) (dist / maxDist);
                float intensity = source.tier().shakeIntensity * distanceFactor;
                if (intensity > maxIntensity) {
                    maxIntensity = intensity;
                }
            }
        }

        currentIntensity = maxIntensity;
    }

    public static float getShakeIntensity() {
        return currentIntensity;
    }

    public static long getShakeSeed() {
        return shakeSeed;
    }

    public static void reset() {
        activeSources.clear();
        currentIntensity = 0f;
    }
}
