package com.targetmaster;

import com.targetmaster.config.TargetMasterConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Formatting;

public class TargetHud {
    public static void init() {
        HudRenderCallback.EVENT.register(TargetHud::render);
    }

    private static void render(DrawContext g, RenderTickCounter tick) {
        TargetMasterConfig cfg = TargetMasterConfig.get();
        if (!cfg.enabled || cfg.hudLayout == 0) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden || mc.currentScreen != null) return;

        Entity entity = TargetMasterConfig.getTargetedEntity();
        if (entity == null) return;

        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        int accent = cfg.glowColor;
        boolean compact = cfg.hudLayout == 1;

        int w = compact ? 140 : 180;
        int h = compact ? 26 : 44;

        int x, y;
        if (cfg.hudPreset == 1) {
            x = 8; y = 8;
        } else if (cfg.hudPreset == 2) {
            x = sw - w - 8; y = 8;
        } else if (cfg.hudPreset == 3) {
            x = sw / 2 - w / 2; y = sh - h - 8;
        } else {
            x = cfg.hudX >= 0 ? cfg.hudX : sw / 2 - w / 2;
            y = cfg.hudY;
        }

        // User size multiplier. (x, y) stays pinned as the panel's top-left
        // anchor; the panel grows right/down from it.
        float scale = Math.max(0.25f, Math.min(3.0f, cfg.hudScale));
        org.joml.Matrix3x2fStack matrices = g.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        matrices.translate(-x, -y);

        if (!compact) {
            // Full layout — original look with pulsing halo
            long time = System.currentTimeMillis();
            float pulse = (float) (Math.sin(time / 180.0) * 0.5 + 0.5);
            int glowAlpha = (int) (60 + pulse * 60);
            int haloColor = (Math.min(255, glowAlpha) << 24) | (accent & 0x00FFFFFF);
            g.fill(x - 4, y - 4, x + w + 4, y + h + 4, haloColor);
        }

        // Frame + accent border
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF2A2D40);
        g.fill(x, y, x + w, y + h, 0xF0101018);
        int borderColor = 0xFF000000 | accent;
        if (compact) {
            // 1px stripe on top in compact mode
            g.fill(x, y, x + w, y + 1, borderColor);
        } else {
            g.fill(x, y, x + w, y + 2, borderColor);
            g.fill(x, y + h - 2, x + w, y + h, borderColor);
            g.fill(x, y, x + 2, y + h, borderColor);
            g.fill(x + w - 2, y, x + w, y + h, borderColor);
        }

        if (!compact) {
            // Full layout: header bar, name + type on one row, bar + bottom hint row
            g.fill(x + 2, y + 2, x + w - 2, y + 18, (0x44 << 24) | (accent & 0x00FFFFFF));
            g.drawTextWithShadow(mc.textRenderer,
                    "✦ " + Formatting.GOLD + "TARGET" + Formatting.WHITE + " ✦",
                    x + 6, y + 6, 0xFFFFFFFF);

            String name = entity.getDisplayName().getString();
            int maxNameW = w - 80;
            if (mc.textRenderer.getWidth(name) > maxNameW) {
                name = mc.textRenderer.trimToWidth(name, maxNameW - mc.textRenderer.getWidth("...")) + "...";
            }
            g.drawTextWithShadow(mc.textRenderer, name, x + 6, y + 21, 0xFFEDEFF7);

            String typeLabel;
            if (entity instanceof PlayerEntity) {
                typeLabel = Formatting.AQUA + "Player";
            } else if (entity instanceof LivingEntity) {
                SpawnGroup cat = entity.getType().getSpawnGroup();
                typeLabel = switch (cat) {
                    case MONSTER -> Formatting.RED + "Hostile";
                    case CREATURE -> Formatting.GREEN + "Animal";
                    case AMBIENT -> Formatting.GRAY + "Ambient";
                    case WATER_CREATURE, WATER_AMBIENT, AXOLOTLS -> Formatting.BLUE + "Aquatic";
                    default -> Formatting.WHITE + "Mob";
                };
            } else {
                typeLabel = Formatting.WHITE + "Entity";
            }
            int tw = mc.textRenderer.getWidth(typeLabel);
            g.drawTextWithShadow(mc.textRenderer, typeLabel, x + w - tw - 6, y + 21, 0xFFFFFFFF);

            if (entity instanceof LivingEntity living) {
                drawBar(g, mc, living, cfg, x, y, w, accent, y + 31, 8);
                double dist = mc.player.distanceTo(entity);
                String distLabel = String.format("%.1fm", dist);
                int dw = mc.textRenderer.getWidth(distLabel);
                g.drawTextWithShadow(mc.textRenderer, distLabel, x + w - dw - 6, y + h - 10, 0xFFA0A4B8);
                g.drawTextWithShadow(mc.textRenderer, Formatting.GOLD + "✦ Highlighted",
                        x + 6, y + h - 10, 0xFFFFD700);
            }
        } else {
            // Compact layout: name + tiny 4px bar on the next line, no halo / no header
            String name = entity.getDisplayName().getString();
            int maxNameW = w - 14;
            if (mc.textRenderer.getWidth(name) > maxNameW) {
                name = mc.textRenderer.trimToWidth(name, maxNameW - mc.textRenderer.getWidth("...")) + "...";
            }
            g.drawTextWithShadow(mc.textRenderer, name, x + 6, y + 5, 0xFFEDEFF7);
            if (entity instanceof LivingEntity living) {
                drawBar(g, mc, living, cfg, x, y, w, accent, y + 18, 4);
            }
        }
        matrices.popMatrix();
    }

    private static void drawBar(DrawContext g, MinecraftClient mc, LivingEntity living,
                                TargetMasterConfig cfg, int x, int y, int w, int accent,
                                int barY, int barH) {
        float maxHp = living.getMaxHealth();
        float hp = Math.max(0, living.getHealth());
        float pct = maxHp > 0 ? hp / maxHp : 0;

        int barX = x + 4;
        int barW = w - 8;

        g.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0A12);
        int fillColor;
        if (pct > 0.5f) fillColor = 0xFF60F0A0;
        else if (pct > 0.25f) fillColor = 0xFFFFD060;
        else fillColor = 0xFFFF6B7A;

        int fillW = Math.max(1, (int) (barW * pct));
        g.fill(barX, barY, barX + fillW, barY + barH, fillColor);
        g.fill(barX, barY, barX + barW, barY + 1, 0x44FFFFFF);

        if (cfg.showHealthText && barH >= 8) {
            String hpText = ((int) hp) + " / " + ((int) maxHp);
            int htW = mc.textRenderer.getWidth(hpText);
            int htX = barX + (barW - htW) / 2;
            int htY = barY + (barH - 8) / 2;
            g.drawTextWithShadow(mc.textRenderer, hpText, htX, htY, 0xFFFFFFFF);
        }
    }
}
