package com.collapsingcaves.cavein;

public enum CaveInTier {
    SMALL("small", 6, 1, 3, 4, 0.125f, 20, 32),
    MEDIUM("medium", 10, 2, 5, 6, 0.25f, 40, 40),
    LARGE("large", 16, 3, 8, 8, 0.375f, 60, 48),
    ENORMOUS("enormous", 24, 4, 12, 10, 0.5f, 80, 56),
    GARGANTUAN("gargantuan", 32, 5, 15, 12, 0.625f, 100, 64);

    public final String id;
    public final int radius;
    public final int maxLayers;
    public final int blocksPerTick;
    public final int layerDelayTicks;
    public final float shakeIntensity;
    public final int shakeDurationTicks;
    public final int shakeMaxDistance;

    CaveInTier(String id, int radius, int maxLayers, int blocksPerTick, int layerDelayTicks,
               float shakeIntensity, int shakeDurationTicks, int shakeMaxDistance) {
        this.id = id;
        this.radius = radius;
        this.maxLayers = maxLayers;
        this.blocksPerTick = blocksPerTick;
        this.layerDelayTicks = layerDelayTicks;
        this.shakeIntensity = shakeIntensity;
        this.shakeDurationTicks = shakeDurationTicks;
        this.shakeMaxDistance = shakeMaxDistance;
    }

    public int getScaledRadius(double multiplier) {
        return Math.max(1, (int) (radius * multiplier));
    }
}
