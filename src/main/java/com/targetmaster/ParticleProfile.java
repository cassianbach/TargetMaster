package com.targetmaster;

import net.minecraft.util.Identifier;

public class ParticleProfile {
    public static final int MAX_COUNT_MULTIPLIER = 128;
    public Identifier particleId;
    public float countMultiplier = 1.0f;
    public float rangeMultiplier = 1.0f;
    public float speedMultiplier = 1.0f;
    public float spreadX = 1.0f;
    public float spreadY = 1.0f;
    public float spreadZ = 1.0f;
    public float gravity = 0.0f;
    public float sizeMultiplier = 1.0f;
    public float offsetX = 0.0f;
    public float offsetY = 0.0f;
    public float offsetZ = 0.0f;
    public int lifetimeOverride = 0;
    public float colorR = -1.0f;
    public float colorG = -1.0f;
    public float colorB = -1.0f;
    public float alphaMultiplier = 1.0f;
    public boolean hasCustomTexture = false;

    public ParticleProfile() {
        this.particleId = null;
    }

    public ParticleProfile(Identifier particleId) {
        this.particleId = particleId;
    }

    public boolean hasColorOverride() {
        return this.colorR >= 0.0f && this.colorG >= 0.0f && this.colorB >= 0.0f;
    }

    public boolean isVanilla() {
        return "minecraft".equals(this.particleId.getNamespace()) && this.countMultiplier == 1.0f && this.rangeMultiplier == 1.0f && this.speedMultiplier == 1.0f && this.spreadX == 1.0f && this.spreadY == 1.0f && this.spreadZ == 1.0f && this.gravity == 0.0f && this.sizeMultiplier == 1.0f && this.offsetX == 0.0f && this.offsetY == 0.0f && this.offsetZ == 0.0f && this.lifetimeOverride == 0 && !this.hasColorOverride() && this.alphaMultiplier == 1.0f && !this.hasCustomTexture;
    }
}
