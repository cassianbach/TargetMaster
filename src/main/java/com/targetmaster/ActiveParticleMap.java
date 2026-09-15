package com.targetmaster;

import com.targetmaster.ParticleProfile;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.particle.Particle;

public final class ActiveParticleMap {
    public static final Map<Particle, ParticleProfile> MAP = new WeakHashMap<Particle, ParticleProfile>();
    public static final Map<Particle, Float> BASE_SCALE = new WeakHashMap<Particle, Float>();

    public static void put(Particle particle, ParticleProfile profile, float baseScale) {
        MAP.put(particle, profile);
        if (baseScale > 0.0f) {
            BASE_SCALE.put(particle, Float.valueOf(baseScale));
        }
    }

    public static void remove(Particle particle) {
        MAP.remove(particle);
        BASE_SCALE.remove(particle);
    }

    private ActiveParticleMap() {
    }
}
