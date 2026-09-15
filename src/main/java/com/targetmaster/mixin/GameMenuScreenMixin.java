package com.targetmaster.mixin;

import com.targetmaster.ParticleTunerClient;
import com.targetmaster.screen.TargetMasterScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin
extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method={"init"}, at={@At(value="TAIL")})
    private void particletuner$addButton(CallbackInfo ci) {
        if (!ParticleTunerClient.CONFIG.escapeButtonVisible) {
            return;
        }
        this.addDrawableChild(ButtonWidget.builder((Text)Text.translatable((String)"menu.particletuner.editor"), button -> MinecraftClient.getInstance().setScreen((Screen)new TargetMasterScreen())).dimensions(this.width / 2 - 102, this.height / 4 + 96 + 24, 204, 20).build());
    }
}
