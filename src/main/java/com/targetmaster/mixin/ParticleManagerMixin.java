package com.targetmaster.mixin;

import com.targetmaster.ActiveParticleMap;
import com.targetmaster.ParticleProfile;
import com.targetmaster.ParticleTunerClient;
import com.targetmaster.mixin.BillboardParticleAccessor;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleManager.class)
public class ParticleManagerMixin {
    private static final ThreadLocal<Integer> RECURSION_GUARD = ThreadLocal.withInitial(() -> 0);

    @Inject(method={"addParticle"}, at={@At(value="HEAD")})
    private void particletuner$applyProfile(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        // TargetMaster's beam spawns its own positioned puffs — never touch those.
        if (com.targetmaster.TargetManager.isBeamSpawning()) return;
        int depth = RECURSION_GUARD.get();
        if (depth > 0 || parameters == null) {
            return;
        }
        Identifier id = Registries.PARTICLE_TYPE.getId(parameters.getType());
        if (id == null) {
            return;
        }
        ParticleProfile profile = ParticleTunerClient.PROFILES.get(id);
        if (profile == null || profile.isVanilla()) {
            return;
        }
        if (profile.countMultiplier > 1.0f) {
            int extra = Math.min((int)profile.countMultiplier - 1, 127);
            RECURSION_GUARD.set(1);
            try {
                ParticleManager self = (ParticleManager)(Object)this;
                for (int i = 0; i < extra; ++i) {
                    self.addParticle(parameters, x, y, z, velocityX, velocityY, velocityZ);
                }
            } finally {
                RECURSION_GUARD.set(0);
            }
        }
    }

    @Inject(method={"addParticle"}, at={@At(value="RETURN")})
    private void particletuner$capture(ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        if (com.targetmaster.TargetManager.isBeamSpawning()) return;
        Particle particle = (Particle)cir.getReturnValue();
        if (particle == null || parameters == null) {
            return;
        }
        Identifier id = Registries.PARTICLE_TYPE.getId(parameters.getType());
        if (id == null) {
            return;
        }
        ParticleProfile profile = ParticleTunerClient.PROFILES.get(id);
        if (profile == null || profile.isVanilla()) {
            return;
        }
        particle.setPos(x + (double)profile.offsetX, y + (double)profile.offsetY, z + (double)profile.offsetZ);
        particle.setVelocity(velocityX * (double)profile.speedMultiplier * (double)profile.spreadX, velocityY * (double)profile.speedMultiplier * (double)profile.spreadY, velocityZ * (double)profile.speedMultiplier * (double)profile.spreadZ);
        if (particle instanceof BillboardParticle) {
            BillboardParticle billboard = (BillboardParticle)particle;
            BillboardParticleAccessor acc = (BillboardParticleAccessor)billboard;
            float baseScale = acc.particletuner$getScale();
            if (profile.hasColorOverride()) {
                acc.particletuner$setRed(profile.colorR);
                acc.particletuner$setGreen(profile.colorG);
                acc.particletuner$setBlue(profile.colorB);
            }
            if (profile.alphaMultiplier != 1.0f) {
                acc.particletuner$setAlpha(acc.particletuner$getAlpha() * profile.alphaMultiplier);
            }
            if (profile.sizeMultiplier != 1.0f) {
                acc.particletuner$setScale(baseScale * profile.sizeMultiplier);
            }
            ActiveParticleMap.put(particle, profile, baseScale);
        }
    }
}
