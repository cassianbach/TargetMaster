package com.targetmaster;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

public class RGBAPickerWidget
extends ClickableWidget {
    private float r;
    private float g;
    private float b;
    private final RGBACallback onChange;

    public RGBAPickerWidget(int x, int y, int w, int h, float r, float g, float b, RGBACallback onChange) {
        super(x, y, w, h, (Text)Text.literal((String)"Color"));
        this.r = r;
        this.g = g;
        this.b = b;
        this.onChange = onChange;
    }

    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float partialTick) {
        int swatchSize = Math.max(40, Math.min(this.height - 4, 60));
        int previewX = this.getX();
        int previewY = this.getY();
        int color = (int)(this.r * 255.0f) << 16 | (int)(this.g * 255.0f) << 8 | (int)(this.b * 255.0f);
        ctx.fill(previewX, previewY, previewX + swatchSize, previewY + swatchSize, 0xFF000000 | color);
        ctx.drawStrokedRectangle(previewX, previewY, swatchSize, swatchSize, -1);
        int sliderX = previewX + swatchSize + 8;
        int sliderW = this.getWidth() - swatchSize - 8;
        int rgbY = previewY;
        this.drawRgbSlider(ctx, sliderX, rgbY + 0, sliderW, 8, this.r, 0xFF2020, -40864);
        this.drawRgbSlider(ctx, sliderX, rgbY + 10, sliderW, 8, this.g, 0x20FF20, -10420384);
        this.drawRgbSlider(ctx, sliderX, rgbY + 20, sliderW, 8, this.b, 0x2020FF, -10460929);
    }

    private void drawRgbSlider(DrawContext ctx, int x, int y, int w, int h, float v, int dark, int light) {
        int darkColor = 0xFF000000 | dark;
        int lightColor = 0xFF000000 | light;
        ctx.fill(x, y, x + w, y + h, darkColor);
        int fill = (int)(v * (float)(w - 1));
        ctx.fill(x, y, x + fill, y + h, lightColor);
        ctx.drawStrokedRectangle(x, y, w, h, -8355712);
    }

    private void push() {
        if (this.onChange != null) {
            this.onChange.onChange(this.r, this.g, this.b);
        }
    }

    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseY;
        double mouseX = click.x();
        if (!this.isMouseOver(mouseX, mouseY = click.y())) {
            return false;
        }
        int swatchSize = Math.max(40, Math.min(this.height - 4, 60));
        int sliderX = this.getX() + swatchSize + 8;
        int sliderW = this.getWidth() - swatchSize - 8;
        if (mouseX < (double)sliderX || mouseX >= (double)(sliderX + sliderW)) {
            return false;
        }
        int idx = (int)((mouseY - (double)this.getY()) / 10.0);
        if (idx < 0 || idx > 2) {
            return false;
        }
        float v = (float)((mouseX - (double)sliderX) / (double)sliderW);
        v = Math.max(0.0f, Math.min(1.0f, v));
        switch (idx) {
            case 0: {
                this.r = v;
                break;
            }
            case 1: {
                this.g = v;
                break;
            }
            case 2: {
                this.b = v;
            }
        }
        this.push();
        return true;
    }

    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }

    public static interface RGBACallback {
        public void onChange(float var1, float var2, float var3);
    }
}
