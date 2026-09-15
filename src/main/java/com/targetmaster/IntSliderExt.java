package com.targetmaster;

import java.util.function.Consumer;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class IntSliderExt
extends SliderWidget {
    private final int min;
    private final int max;
    private final String label;
    private Consumer<Integer> onChange;

    private IntSliderExt(int x, int y, int w, int h, String label, int min, int max, int value) {
        super(x, y, w, h, (Text)Text.empty(), IntSliderExt.toFrac(value, min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.updateMessage();
    }

    private static double toFrac(int v, int min, int max) {
        if (max <= min) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double)(v - min) / (double)(max - min)));
    }

    private int getValue() {
        return (int)Math.round((double)this.min + (double)(this.max - this.min) * this.value);
    }

    protected void updateMessage() {
        this.setMessage((Text)Text.literal((String)(this.label + ": " + this.getValue())));
    }

    protected void applyValue() {
        if (this.onChange != null) {
            this.onChange.accept(this.getValue());
        }
    }

    public IntSliderExt onChange(Consumer<Integer> c) {
        this.onChange = c;
        return this;
    }

    public static Builder builder(String label, int min, int max, int step) {
        return new Builder(label, min, max, step);
    }

    public static class Builder {
        private final String label;
        private final int min;
        private final int max;
        private final int step;
        private int x;
        private int y;
        private int w;
        private int h = 20;
        private int start = Integer.MIN_VALUE;
        private Consumer<Integer> consumer;

        Builder(String label, int min, int max, int step) {
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

        public Builder setValue(int v) {
            this.start = v;
            return this;
        }

        public Builder onChange(Consumer<Integer> c) {
            this.consumer = c;
            return this;
        }

        public IntSliderExt build() {
            int v = this.start == Integer.MIN_VALUE ? this.min : this.start;
            IntSliderExt s = new IntSliderExt(this.x, this.y, this.w, this.h, this.label, this.min, this.max, v);
            s.onChange = this.consumer;
            return s;
        }
    }
}
