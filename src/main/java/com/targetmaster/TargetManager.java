package com.targetmaster;

import com.targetmaster.config.TargetMasterConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.particle.TintedParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public class TargetManager {
    private static int targetId = -1;
    private static int tickCounter = 0;
    /**
     * Set while the target beam is spawning its own particles. The ParticleTuner
     * mixin checks this and leaves beam particles alone — otherwise a tuned
     * profile (offsetX/Y/Z, spread, count) would shift the whole beam sideways.
     */
    private static final ThreadLocal<Boolean> BEAM_SPAWNING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean isBeamSpawning() {
        return BEAM_SPAWNING.get();
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(TargetManager::onEndTick);
    }

    public static void setTarget(Entity entity) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        if (entity == null) {
            targetId = -1;
            return;
        }
        targetId = entity.getId();
        tickCounter = 0;
    }

    public static void clear() {
        setTarget(null);
    }

    public static int getTargetId() {
        return targetId;
    }

    private static void onEndTick(MinecraftClient mc) {
        if (mc.world == null || mc.player == null) return;
        TargetMasterConfig cfg = TargetMasterConfig.get();
        if (!cfg.enabled) {
            if (targetId >= 0) targetId = -1;
            return;
        }
        if (targetId < 0) return;
        Entity e = mc.world.getEntityById(targetId);
        if (e == null || !e.isAlive()) {
            targetId = -1;
            return;
        }
        tickCounter++;

        if (cfg.particleDensity <= 0) return;
        if (tickCounter % cfg.particleDensity != 0) return;
        spawnBeam(mc, e, cfg.glowColor);
    }

    private static void spawnBeam(MinecraftClient mc, Entity entity, int color) {
        ClientWorld world = mc.world;
        if (world == null) return;

        TargetMasterConfig cfg = TargetMasterConfig.get();
        ParticleEffect marker = resolveMarkerParticle(cfg.markerParticle, color);

        double baseX = entity.getX();
        double baseY = entity.getY();
        double topY = entity.getY() + entity.getHeight();
        double width = Math.max(entity.getWidth() * 0.55, 0.4);
        double beamH = Math.max(2.0, cfg.beamHeight);
        int beamSteps = Math.max(2, cfg.beamSteps);
        int ringCount = Math.max(3, cfg.ringCount);

        // Perfectly vertical column: no sideways jitter, no drift velocity.
        // Any lean over an 18-block pillar reads as "warped a block sideways",
        // so every puff spawns exactly above the mob's center and stays put.
        // Guard flag keeps ParticleTuner profiles (offsets/spread) off these.
        BEAM_SPAWNING.set(Boolean.TRUE);
        try {
            for (int i = 0; i < beamSteps; i++) {
                double frac = i / (double) (beamSteps - 1);
                double y = topY + frac * beamH;
                world.addParticleClient(marker, baseX, y, entity.getZ(), 0, 0, 0);
            }

            double ringRadius = width * 1.3;
            for (int i = 0; i < ringCount; i++) {
                double t = i / (double) ringCount * Math.PI * 2.0 + tickCounter * 0.06;
                double rx = baseX + Math.cos(t) * ringRadius;
                double rz = entity.getZ() + Math.sin(t) * ringRadius;
                world.addParticleClient(marker, rx, baseY + 0.05, rz, 0, 0, 0);
            }
        } finally {
            BEAM_SPAWNING.set(Boolean.FALSE);
        }
    }

    @SuppressWarnings("unchecked")
    private static ParticleEffect resolveMarkerParticle(String idStr, int color) {
        Identifier id;
        try {
            id = Identifier.tryParse(idStr);
        } catch (Exception e) {
            id = null;
        }
        if (id == null) id = Identifier.ofVanilla("dust");
        if (!Registries.PARTICLE_TYPE.containsId(id)) {
            return new DustParticleEffect(color, 1.0f);
        }
        ParticleType<?> type = Registries.PARTICLE_TYPE.get(id);

        if (type instanceof SimpleParticleType spt) {
            return spt;
        }
        if ("dust".equals(id.getPath())) {
            return new DustParticleEffect(color, 1.0f);
        }
        if ("entity_effect".equals(id.getPath()) || "effect".equals(id.getPath())) {
            return TintedParticleEffect.create((ParticleType<TintedParticleEffect>) type, color);
        }
        // Fallback
        return new DustParticleEffect(color, 1.0f);
    }
}
