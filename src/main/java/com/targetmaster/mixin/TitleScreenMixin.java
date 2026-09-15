package com.targetmaster.mixin;

import com.targetmaster.ParticleTunerClient;
import com.targetmaster.screen.TargetMasterScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin
extends Screen {
    protected TitleScreenMixin(Text title) {
        super(title);
    }

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void particletuner$addButton(CallbackInfo ci) {
        if (!ParticleTunerClient.CONFIG.titleButtonVisible) {
            return;
        }
        this.addDrawableChild(ButtonWidget.builder((Text)Text.translatable((String)"menu.particletuner.editor"), button -> MinecraftClient.getInstance().setScreen((Screen)new TargetMasterScreen())).dimensions(this.width - 110, 6, 100, 20).build());
    }
}
