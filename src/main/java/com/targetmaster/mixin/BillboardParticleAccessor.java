package com.targetmaster.mixin;

import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.texture.Sprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BillboardParticle.class)
public interface BillboardParticleAccessor {
    @Accessor(value="sprite")
    public Sprite particletuner$getSprite();

    @Accessor(value="red")
    public float particletuner$getRed();

    @Accessor(value="red")
    public void particletuner$setRed(float var1);

    @Accessor(value="green")
    public float particletuner$getGreen();

    @Accessor(value="green")
    public void particletuner$setGreen(float var1);

    @Accessor(value="blue")
    public float particletuner$getBlue();

    @Accessor(value="blue")
    public void particletuner$setBlue(float var1);

    @Accessor(value="alpha")
    public float particletuner$getAlpha();

    @Accessor(value="alpha")
    public void particletuner$setAlpha(float var1);

    @Accessor(value="scale")
    public float particletuner$getScale();

    @Accessor(value="scale")
    public void particletuner$setScale(float var1);
}
