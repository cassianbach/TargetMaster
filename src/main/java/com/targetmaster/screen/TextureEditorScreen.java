package com.targetmaster.screen;

import com.targetmaster.FloatSliderExt;
import com.targetmaster.ParticleProfile;
import com.targetmaster.ParticleTunerClient;
import com.targetmaster.ProfileStorage;
import com.targetmaster.RGBAPickerWidget;
import com.targetmaster.TextureStorage;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

public class TextureEditorScreen extends Screen {
    private final Screen parent;
    private final Identifier particleId;
    private final int size;
    private int[] pixels;
    private int currentColor = -1;
    private int previewX = 0;
    private int previewY = 0;
    private final Deque<int[]> undo = new ArrayDeque<>();
    private final Deque<int[]> redo = new ArrayDeque<>();
    private final boolean repixelable;

    public TextureEditorScreen(Screen parent, Identifier particleId) {
        super(Text.translatable("screen.particletuner.editor.title", particleId.toString()));
        this.parent = parent;
        this.particleId = particleId;
        this.size = ParticleTunerClient.CONFIG.defaultTextureSize;
        this.repixelable = TextureStorage.isRepixelable(particleId);
        ParticleProfile profile = ParticleTunerClient.PROFILES.get(particleId);
        if (profile != null && profile.hasColorOverride()) {
            int r = Math.max(0, Math.min(255, (int) (profile.colorR * 255.0f)));
            int g = Math.max(0, Math.min(255, (int) (profile.colorG * 255.0f)));
            int b = Math.max(0, Math.min(255, (int) (profile.colorB * 255.0f)));
            this.currentColor = 0xFF000000 | r << 16 | g << 8 | b;
        }
        this.pixels = TextureStorage.loadOrCreateBuffer(particleId, this.size).clone();
    }

    @Override
    protected void init() {
        int gridX = this.width / 2 - this.size * 8;
        int gridY = 40;
        this.previewX = gridX;
        this.previewY = gridY;
        int controlsX = 20;
        int y = 40;
        ParticleProfile profile = ParticleTunerClient.PROFILES.get(this.particleId);
        boolean hasColor = profile != null && profile.hasColorOverride();
        float r = hasColor ? profile.colorR : 1.0f;
        float g = hasColor ? profile.colorG : 1.0f;
        float b = hasColor ? profile.colorB : 1.0f;
        RGBAPickerWidget.RGBACallback colorSetter = (nr, ng, nb) -> {
            this.currentColor = 0xFF000000 | ((int) (nr * 255.0f) << 16 | (int) (ng * 255.0f) << 8 | (int) (nb * 255.0f));
            ParticleProfile p = ParticleTunerClient.PROFILES.computeIfAbsent(this.particleId, ParticleProfile::new);
            p.colorR = nr;
            p.colorG = ng;
            p.colorB = nb;
            ProfileStorage.saveProfiles(ParticleTunerClient.PROFILES);
        };
        RGBAPickerWidget picker = new RGBAPickerWidget(controlsX, y, 220, 32, r, g, b, colorSetter);
        this.addDrawableChild(picker);
        FloatSliderExt opacitySlider = FloatSliderExt.builder(Text.translatable("field.particletuner.opacity").getString(), 0.0f, 1.0f, 0.01f).bounds(controlsX, y += 38, 220, 20).setValue((float) (this.currentColor >>> 24 & 0xFF) / 255.0f).onChange(v -> {
            int a = Math.max(0, Math.min(255, (int) (v * 255.0f)));
            this.currentColor = this.currentColor & 0xFFFFFF | a << 24;
        }).build();
        this.addDrawableChild(opacitySlider);
        y += 28;
        int btnW = 70;
        int bottomY = this.height - 28;
        int x = 20;
        if (this.repixelable) {
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.particletuner.fill"), btn -> {
                this.snapshot();
                Arrays.fill(this.pixels, this.currentColor);
            }).dimensions(x, bottomY, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.particletuner.clear"), btn -> {
                this.snapshot();
                Arrays.fill(this.pixels, 0);
            }).dimensions(x += btnW + 4, bottomY, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.particletuner.undo"), btn -> {
                if (!this.undo.isEmpty()) {
                    this.redo.push(this.pixels);
                    this.pixels = this.undo.pop();
                }
            }).dimensions(x += btnW + 4, bottomY, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal(">>"), btn -> TextureStorage.reloadResources()).dimensions(x += btnW + 4, bottomY, btnW, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("button.particletuner.save"), btn -> {
                this.snapshot();
                TextureStorage.save(this.particleId, this.size, this.pixels);
                ParticleProfile p = ParticleTunerClient.PROFILES.computeIfAbsent(this.particleId, ParticleProfile::new);
                p.hasCustomTexture = true;
                this.close();
            }).dimensions(x += btnW + 4, bottomY, btnW, 20).build());
            x += btnW + 4;
        }
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), btn -> this.close()).dimensions(x, bottomY, btnW, 20).build());
    }

    private void snapshot() {
        this.undo.push(this.pixels.clone());
        if (this.undo.size() > 32) {
            this.undo.removeLast();
        }
        this.redo.clear();
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.drawText(this.textRenderer, this.title, 20, 4, -1, false);
        for (int y = 0; y < this.size; ++y) {
            for (int x = 0; x < this.size; ++x) {
                int c = this.pixels[y * this.size + x];
                g.fill(this.previewX + x * 8, this.previewY + y * 8, this.previewX + x * 8 + 8, this.previewY + y * 8 + 8, c);
            }
        }
        g.drawStrokedRectangle(this.previewX, this.previewY, this.size * 8, this.size * 8, -1);
        g.fill(this.width - 60, 40, this.width - 20, 80, this.currentColor);
        g.drawStrokedRectangle(this.width - 60, 40, 40, 40, -1);
        if (!this.repixelable) {
            g.drawText(this.textRenderer, Text.literal("This particle uses block textures; texture repixel is not available."), 20, this.height / 2, -21931, false);
        }
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (mouseX >= (double) this.previewX && mouseX < (double) (this.previewX + this.size * 8) && mouseY >= (double) this.previewY && mouseY < (double) (this.previewY + this.size * 8)) {
            int px = (int) ((mouseX - (double) this.previewX) / 8.0);
            int py = (int) ((mouseY - (double) this.previewY) / 8.0);
            this.snapshot();
            this.pixels[py * this.size + px] = this.currentColor;
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (mouseX >= (double) this.previewX && mouseX < (double) (this.previewX + this.size * 8) && mouseY >= (double) this.previewY && mouseY < (double) (this.previewY + this.size * 8)) {
            int px = (int) ((mouseX - (double) this.previewX) / 8.0);
            int py = (int) ((mouseY - (double) this.previewY) / 8.0);
            if (px >= 0 && py >= 0 && px < this.size && py < this.size) {
                this.pixels[py * this.size + px] = this.currentColor;
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public void close() {
        ProfileStorage.saveProfiles(ParticleTunerClient.PROFILES);
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
