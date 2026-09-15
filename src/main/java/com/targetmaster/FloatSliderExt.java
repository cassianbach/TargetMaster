package com.targetmaster;

import java.util.function.Consumer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class FloatSliderExt
extends SliderWidget {
    private final float min;
    private final float max;
    private final float step;
    private final String label;
    private Consumer<Float> onChange;

    private FloatSliderExt(int x, int y, int w, int h, String label, float min, float max, float step, float value) {
        super(x, y, w, h, (Text)Text.empty(), FloatSliderExt.toFrac(value, min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.updateMessage();
    }

    private static double toFrac(float v, float min, float max) {
        if (max <= min) {
            return 0.0;
        }
        double f = (v - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, f));
    }

    private float getValue() {
        return (float)((double)this.min + (double)(this.max - this.min) * this.value);
    }

    protected void updateMessage() {
        String s = String.format("%s: %.2f", this.label, Float.valueOf(this.getValue()));
        this.setMessage((Text)Text.literal((String)s));
    }

    protected void applyValue() {
        float v = this.getValue();
        if (this.step > 0.0f) {
            v = (float)Math.round(v / this.step) * this.step;
        }
        if (this.onChange != null) {
            this.onChange.accept(Float.valueOf(v));
        }
    }

    public FloatSliderExt onChange(Consumer<Float> c) {
        this.onChange = c;
        return this;
    }

    public static Builder builder(String label, float min, float max, float step) {
        return new Builder(label, min, max, step);
    }

    public static class Builder {
        private final String label;
        private final float min;
        private final float max;
        private final float step;
        private int x;
        private int y;
        private int w;
        private int h = 20;
        private float start = Float.NaN;
        private Consumer<Float> consumer;

        Builder(String label, float min, float max, float step) {
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public Builder bounds(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            return this;
        }

        public Builder setValue(float v) {
            this.start = v;
            return this;
        }

        public Builder onChange(Consumer<Float> c) {
            this.consumer = c;
            return this;
        }

        public FloatSliderExt build() {
            float v = Float.isNaN(this.start) ? this.min : this.start;
            FloatSliderExt s = new FloatSliderExt(this.x, this.y, this.w, this.h, this.label, this.min, this.max, this.step, v);
            s.onChange = this.consumer;
            return s;
        }
    }
}
