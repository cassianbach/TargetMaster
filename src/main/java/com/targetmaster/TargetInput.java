package com.targetmaster;

import com.targetmaster.config.TargetMasterConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class TargetInput {
    public static final KeyBinding OPEN_MENU = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.targetmaster.menu", GLFW.GLFW_KEY_G, KeyBinding.Category.MISC));
    public static final KeyBinding CLEAR_TARGET = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.targetmaster.clear", GLFW.GLFW_KEY_H, KeyBinding.Category.MISC));
    // Keybindable target select: targets whatever the crosshair is on.
    // Rebindable in Controls like any vanilla key (default R).
    public static final KeyBinding TARGET_SELECT = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.targetmaster.select", GLFW.GLFW_KEY_R, KeyBinding.Category.MISC));

    private static boolean wasRightDown = false;
    private static boolean wasLeftDown = false;
    private static boolean dragging = false;
    private static double dragStartMouseX, dragStartMouseY;
    private static int dragStartHudX, dragStartHudY;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(TargetInput::onTick);
    }

    private static void onTick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null) return;

        while (OPEN_MENU.wasPressed()) {
            mc.setScreen(new com.targetmaster.screen.TargetMasterScreen());
        }
        while (CLEAR_TARGET.wasPressed()) {
            TargetManager.clear();
        }

        if (mc.currentScreen != null) {
            wasRightDown = false;
            wasLeftDown = false;
            dragging = false;
            return;
        }

        // Keybind target select (same as right-click targeting)
        while (TARGET_SELECT.wasPressed()) {
            if (TargetMasterConfig.get().enabled) {
                tryTargetFromLook(mc);
            }
        }

        long window = mc.getWindow().getHandle();

        // Right click = target an entity
        boolean rightDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        if (rightDown && !wasRightDown) {
            tryTargetFromLook(mc);
        }
        wasRightDown = rightDown;

        // Left click + drag on the HUD panel = move HUD
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        TargetMasterConfig cfg = TargetMasterConfig.get();
        if (!cfg.enabled || cfg.hudLayout == 0) {
            wasLeftDown = leftDown;
            return;
        }

        double[] cursor = resolveCursor(window, mc);
        double mx = cursor[0];
        double my = cursor[1];

        if (leftDown && !wasLeftDown) {
            int[] rect = hudRect(mc, cfg);
            if (mx >= rect[0] && mx < rect[0] + rect[2] && my >= rect[1] && my < rect[1] + rect[3]) {
                dragging = true;
                dragStartMouseX = mx;
                dragStartMouseY = my;
                dragStartHudX = cfg.hudX >= 0 ? cfg.hudX : rect[0];
                dragStartHudY = cfg.hudY;
            }
        } else if (!leftDown) {
            if (dragging) {
                dragging = false;
                TargetMasterConfig.save();
            }
        } else if (dragging) {
            int dx = (int) (mx - dragStartMouseX);
            int dy = (int) (my - dragStartMouseY);
            // When dragging, switch to manual placement (no preset)
            cfg.hudPreset = 0;
            cfg.hudX = dragStartHudX + dx;
            cfg.hudY = dragStartHudY + dy;
        }
        wasLeftDown = leftDown;
    }

    private static int[] hudRect(MinecraftClient mc, TargetMasterConfig cfg) {
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        boolean compact = cfg.hudLayout == 1;
        float scale = Math.max(0.25f, Math.min(3.0f, cfg.hudScale));
        int w = (int) ((compact ? 140 : 180) * scale);
        int h = (int) ((compact ? 26 : 44) * scale);
        int x, y;
        switch (cfg.hudPreset) {
            case 1 -> { x = 8; y = 8; }
            case 2 -> { x = sw - w - 8; y = 8; }
            case 3 -> { x = sw / 2 - w / 2; y = sh - h - 8; }
            default -> {
                x = cfg.hudX >= 0 ? cfg.hudX : sw / 2 - w / 2;
                y = cfg.hudY;
            }
        }
        return new int[]{x, y, w, h};
    }

    private static double[] resolveCursor(long window, MinecraftClient mc) {
        double[] x = new double[1];
        double[] y = new double[1];
        GLFW.glfwGetCursorPos(window, x, y);
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        int fbw = mc.getWindow().getFramebufferWidth();
        int fbh = mc.getWindow().getFramebufferHeight();
        double sx = sw / (double) fbw;
        double sy = sh / (double) fbh;
        return new double[]{x[0] * sx, y[0] * sy};
    }

    private static void tryTargetFromLook(MinecraftClient mc) {
        PlayerEntity player = mc.player;
        if (player == null) return;
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr) {
            Entity ent = ehr.getEntity();
            if (TargetMasterConfig.isTargetable(ent)) {
                if (TargetManager.getTargetId() == ent.getId()) {
                    TargetManager.clear();
                } else {
                    TargetManager.setTarget(ent);
                }
            }
        }
    }
}
