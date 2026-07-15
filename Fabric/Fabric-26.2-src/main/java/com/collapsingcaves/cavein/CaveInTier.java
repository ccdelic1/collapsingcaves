package com.collapsingcaves.cavein;

public enum CaveInTier {
    SMALL("small", 6, 1, 4, 0.125f, 32),
    MEDIUM("medium", 10, 2, 6, 0.25f, 40),
    LARGE("large", 16, 3, 8, 0.375f, 48),
    ENORMOUS("enormous", 24, 4, 10, 0.5f, 56),
    GARGANTUAN("gargantuan", 32, 5, 12, 0.625f, 64);

    public final String id;
    public final int radius;
    public final int maxLayers;
    public final int layerDelayTicks;
    public final float shakeIntensity;
    public final int shakeMaxDistance;

    CaveInTier(String id, int radius, int maxLayers, int layerDelayTicks,
               float shakeIntensity, int shakeMaxDistance) {
        this.id = id;
        this.radius = radius;
        this.maxLayers = maxLayers;
        this.layerDelayTicks = layerDelayTicks;
        this.shakeIntensity = shakeIntensity;
        this.shakeMaxDistance = shakeMaxDistance;
    }

    public int getScaledRadius(double multiplier) {
        return Math.max(1, (int) (radius * multiplier));
    }
}
