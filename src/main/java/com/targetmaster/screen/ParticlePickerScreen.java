package com.targetmaster.screen;

import com.targetmaster.ParticleCatalog;
import com.targetmaster.config.TargetMasterConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Sub-screen for picking the marker particle used by the TargetMaster beam.
 * Pulls from the same ParticleCatalog as ParticleTuner.
 */
public class ParticlePickerScreen extends Screen {
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 300;
    private static final int CORNER = 4;

    private final Screen parent;
    private int listScroll = 0;
    private TextFieldWidget search;

    public ParticlePickerScreen(Screen parent) {
        super(Text.literal("Pick Marker Particle"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - GUI_WIDTH) / 2;
        int top = (this.height - GUI_HEIGHT) / 2;

        this.search = new TextFieldWidget(this.textRenderer, left + 8, top + 22, GUI_WIDTH - 16, 16, Text.literal("Search"));
        this.search.setPlaceholder(Text.literal("Search particles…"));
        this.addDrawableChild(this.search);

        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(left + 8, top + GUI_HEIGHT - 28, GUI_WIDTH - 16, 20).build());
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xDD05050A);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        int left = (this.width - GUI_WIDTH) / 2;
        int top = (this.height - GUI_HEIGHT) / 2;

        g.fill(left, top, left + GUI_WIDTH, top + GUI_HEIGHT, 0xE0101018);
        g.fill(left, top, left + GUI_WIDTH, top + 1, 0xFF3A3D55);
        g.fill(left, top + GUI_HEIGHT - 1, left + GUI_WIDTH, top + GUI_HEIGHT, 0xFF2A2D40);
        g.fill(left, top, left + 1, top + GUI_HEIGHT, 0xFF3A3D55);
        g.fill(left + GUI_WIDTH - 1, top, left + GUI_WIDTH, top + GUI_HEIGHT, 0xFF2A2D40);

        String title = "Pick Marker Particle";
        int tw = textRenderer.getWidth(title);
        g.drawText(textRenderer, "✦ " + title + " ✦", left + (GUI_WIDTH - tw) / 2, top + 6, 0xFFFFD700, true);

        int listX = left + 8;
        int listY = top + 44;
        int listW = GUI_WIDTH - 16;
        int listH = GUI_HEIGHT - 80;

        g.fill(listX - 2, listY - 2, listX + listW + 2, listY + listH + 2, 0xFF05050A);
        g.enableScissor(listX, listY, listX + listW, listY + listH);

        int y = listY - listScroll;
        String query = search != null ? search.getText().toLowerCase() : "";
        String current = TargetMasterConfig.get().markerParticle;
        for (Map.Entry<String, List<Identifier>> ns : ParticleCatalog.groupedByNamespace().entrySet()) {
            g.drawText(textRenderer, "── " + ns.getKey() + " ──", listX + 4, y, 0xFFA0A4B8, false);
            y += 14;
            for (Identifier id : ns.getValue()) {
                if (!query.isEmpty() && !id.toString().toLowerCase().contains(query)) continue;
                String idStr = id.toString();
                boolean sel = idStr.equals(current);
                if (sel) {
                    g.fill(listX, y - 1, listX + listW, y + 11, 0xFF2D3148);
                    g.fill(listX, y - 1, listX + 3, y + 11, 0xFFFFD700);
                }
                if (y + 12 > listY && y < listY + listH) {
                    g.drawText(textRenderer, id.getPath(), listX + 4, y, sel ? 0xFFFFD700 : 0xFFFFFFFF, false);
                    g.drawText(textRenderer, id.getNamespace(), listX + listW - textRenderer.getWidth(id.getNamespace()) - 4, y, 0xFF6B7080, false);
                }
                y += 12;
            }
        }
        g.disableScissor();

        // Hint at bottom
        String hint = "Particle used for the target beam + ground ring";
        if (textRenderer.getWidth(hint) > GUI_WIDTH - 16) hint = textRenderer.trimToWidth(hint, GUI_WIDTH - 24) + "…";
        g.drawText(textRenderer, hint, listX, top + GUI_HEIGHT - 56, 0xFFA0A4B8, false);

        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;

        int left = (this.width - GUI_WIDTH) / 2;
        int top = (this.height - GUI_HEIGHT) / 2;
        int listX = left + 8;
        int listY = top + 44;
        int listW = GUI_WIDTH - 16;
        int listH = GUI_HEIGHT - 80;

        double mx = click.x();
        double my = click.y();
        if (mx >= listX && mx < listX + listW && my >= listY && my < listY + listH) {
            int y = listY - listScroll;
            String query = search != null ? search.getText().toLowerCase() : "";
            for (Map.Entry<String, List<Identifier>> ns : ParticleCatalog.groupedByNamespace().entrySet()) {
                y += 14;
                for (Identifier id : ns.getValue()) {
                    if (!query.isEmpty() && !id.toString().toLowerCase().contains(query)) continue;
                    if (my >= y && my < y + 12) {
                        TargetMasterConfig.get().markerParticle = id.toString();
                        TargetMasterConfig.save();
                        return true;
                    }
                    y += 12;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        int left = (this.width - GUI_WIDTH) / 2;
        int top = (this.height - GUI_HEIGHT) / 2;
        int listX = left + 8;
        int listY = top + 44;
        int listW = GUI_WIDTH - 16;
        int listH = GUI_HEIGHT - 80;
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            listScroll = Math.max(0, (int) (listScroll - verticalAmount * 12));
            return true;
        }
        return false;
    }

    @Override
    public void close() {
        TargetMasterConfig.save();
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
