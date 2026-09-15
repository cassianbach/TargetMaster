package com.targetmaster.mixin;

import com.targetmaster.ActiveParticleMap;
import com.targetmaster.ParticleProfile;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.BillboardParticleSubmittable;
import net.minecraft.client.texture.Sprite;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BillboardParticle.class)
public class BillboardParticleMixin {
    @Shadow
    public float red;
    @Shadow
    public float green;
    @Shadow
    public float blue;
    @Shadow
    public float alpha;
    @Shadow
    public float scale;
    @Shadow
    protected Sprite sprite;

    @Inject(method={"renderVertex"}, at={@At(value="HEAD")})
    private void particletuner$applyOverrides(BillboardParticleSubmittable submittable, Quaternionf quat, float x, float y, float z, float tickDelta, CallbackInfo ci) {
        Float base;
        BillboardParticle self = (BillboardParticle)(Object)this;
        ParticleProfile profile = ActiveParticleMap.MAP.get(self);
        if (profile == null) {
            return;
        }
        if (profile.hasColorOverride()) {
            this.red = profile.colorR;
            this.green = profile.colorG;
            this.blue = profile.colorB;
        }
        if (profile.alphaMultiplier != 1.0f) {
            this.alpha = profile.alphaMultiplier;
        }
        if ((base = ActiveParticleMap.BASE_SCALE.get(self)) != null) {
            this.scale = base.floatValue() * profile.sizeMultiplier;
        }
    }
}
