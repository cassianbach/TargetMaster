package com.targetmaster.mixin;

import com.targetmaster.ActiveParticleMap;
import com.targetmaster.ParticleProfile;
import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Particle.class)
public class ParticleMixin {
    @Shadow
    public float gravityStrength;
    @Shadow
    public int maxAge;

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void particletuner$applyGravity(CallbackInfo ci) {
        ParticleProfile profile = ActiveParticleMap.MAP.get(this);
        if (profile != null) {
            this.gravityStrength = profile.gravity;
            if (profile.lifetimeOverride > 0) {
                this.maxAge = profile.lifetimeOverride;
            }
        }
    }
}
