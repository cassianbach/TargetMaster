package com.targetmaster.screen;

import com.targetmaster.ParticleCatalog;
import com.targetmaster.ParticleProfile;
import com.targetmaster.ParticleTunerClient;
import com.targetmaster.ProfileStorage;
import com.targetmaster.TextureStorage;
import com.targetmaster.config.TargetMasterConfig;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Unified MobMastery-styled menu for both TargetMaster and ParticleTuner.
 * Top tabs toggle between the two feature sets — the rest of the chrome
 * (gold-accented title, dark panels, scroll bar, shimmer) is shared.
 */
public class TargetMasterScreen extends Screen {
    private static final int GUI_WIDTH = 480;
    private static final int GUI_HEIGHT = 400;
    private static final int PANEL_MARGIN = 10;
    private static final int CORNER = 4;
    private static final int TAB_BAR_HEIGHT = 24;

    private static final int COL_BG_DARK = 0xE0101018;
    private static final int COL_BG_PANEL = 0xCC14141F;
    private static final int COL_BG_PANEL_HIDDEN = 0xCC1A0F2A;
    private static final int COL_BG_HEADER = 0xEE0A0A14;
    private static final int COL_BORDER_LIGHT = 0xFF3A3D55;
    private static final int COL_BORDER = 0xFF2A2D40;
    private static final int COL_ACCENT_GOLD = 0xFFFFD700;
    private static final int COL_ACCENT_VOID = 0xFFB070FF;
    private static final int COL_ACCENT_CYAN = 0xFF70E0FF;
    private static final int COL_TEXT_PRIMARY = 0xFFEDEFF7;
    private static final int COL_TEXT_SECONDARY = 0xFFA0A4B8;
    private static final int COL_TEXT_MUTED = 0xFF6B7080;
    private static final int COL_GLOW_SOFT = 0x44FFD700;
    private static final int COL_GLOW_VOID = 0x55B070FF;
    private static final int COL_HOVER = 0xFF222638;
    private static final int COL_SELECTED = 0xFF2D3148;

    private int guiLeft, guiTop;
    private int currentTab = TAB_TARGETS;

    private static final int TAB_TARGETS = 0;
    private static final int TAB_PARTICLES = 1;

    // ---- particle editor state (PT) ----
    private int particleListScroll = 0;
    private Identifier selectedParticle;
    private TextFieldWidget search;

    // ---- widget tracking ----
    // Every widget we add is tracked per-tab so tab switches (and re-inits)
    // can remove + re-add exactly the right set. Nothing is ever rebuilt
    // implicitly, so nothing can pile up, go stale, or swallow clicks.
    private final java.util.List<net.minecraft.client.gui.widget.ClickableWidget> targetsWidgets = new java.util.ArrayList<>();
    private final java.util.List<net.minecraft.client.gui.widget.ClickableWidget> particlesWidgets = new java.util.ArrayList<>();
    private final java.util.List<net.minecraft.client.gui.widget.ClickableWidget> particleSliderWidgets = new java.util.ArrayList<>();
    private net.minecraft.client.gui.widget.ButtonWidget doneButton;
    private java.util.List<net.minecraft.client.gui.widget.ClickableWidget> buildList;

    public TargetMasterScreen() {
        super(Text.literal("TargetMaster"));
    }

    @Override
    protected void init() {
        guiLeft = (this.width - GUI_WIDTH) / 2;
        guiTop = (this.height - GUI_HEIGHT) / 2;
        // Full rebuild is safe here: init() never calls clearAndInit(), so no
        // recursion. Tracked widgets are removed first, so resize re-inits
        // can't duplicate anything either.
        for (net.minecraft.client.gui.widget.ClickableWidget w : targetsWidgets) this.remove(w);
        for (net.minecraft.client.gui.widget.ClickableWidget w : particlesWidgets) this.remove(w);
        if (doneButton != null) this.remove(doneButton);
        targetsWidgets.clear();
        particlesWidgets.clear();
        particleSliderWidgets.clear();
        buildList = targetsWidgets;
        buildTargetsWidgets();
        buildList = particlesWidgets;
        buildParticlesWidgets();
        buildList = null;
        addDoneButton();
        refreshTabWidgets();
    }

    /** Track + register a widget on the tab currently being built. */
    private <T extends net.minecraft.client.gui.widget.ClickableWidget> T add(T w) {
        addDrawableChild(w);
        if (buildList != null) buildList.add(w);
        return w;
    }

    /** Show only the active tab's widgets. No rebuilds, no stale overlays. */
    private void refreshTabWidgets() {
        for (net.minecraft.client.gui.widget.ClickableWidget w : targetsWidgets) this.remove(w);
        for (net.minecraft.client.gui.widget.ClickableWidget w : particlesWidgets) this.remove(w);
        java.util.List<net.minecraft.client.gui.widget.ClickableWidget> show =
                (currentTab == TAB_TARGETS) ? targetsWidgets : particlesWidgets;
        for (net.minecraft.client.gui.widget.ClickableWidget w : show) this.addDrawableChild(w);
    }

    private void switchTab(int tab) {
        if (tab == currentTab) return;
        currentTab = tab;
        refreshTabWidgets();
    }

    /** Tab strip bounds: {x0, x1} for tab index 0/1, y0, y1. */
    private int tabStripY0() { return guiTop + 20; }
    private int tabStripY1() { return guiTop + 20 + TAB_BAR_HEIGHT; }
    private int tabStripW() { return (GUI_WIDTH - 2) / 2; }

    private void renderTabBar(DrawContext g) {
        int fx = guiLeft;
        int fw = GUI_WIDTH;
        int tabY = guiTop + 20;
        int tabH = 14;
        g.fill(fx, tabY, fx + fw, tabY + 1, COL_BORDER_LIGHT);
        int tabCount = 2;
        int tabW = (fw - 2) / tabCount;
        String[] tabs = {"Targets", "Particles"};
        for (int i = 0; i < tabCount; i++) {
            int tx = fx + 1 + i * tabW;
            boolean selected = (currentTab == TAB_TARGETS && i == 0) || (currentTab == TAB_PARTICLES && i == 1);
            int bg = selected ? COL_BG_PANEL : 0x88141826;
            int textCol = selected ? COL_ACCENT_GOLD : COL_TEXT_MUTED;
            g.fill(tx, tabY + 1, tx + tabW, tabY + tabH, bg);
            if (selected) {
                g.fill(tx, tabY + tabH - 1, tx + tabW, tabY + tabH, COL_ACCENT_GOLD);
                g.fill(tx, tabY + 1, tx + 1, tabY + tabH, COL_ACCENT_GOLD);
                g.fill(tx + tabW - 1, tabY + 1, tx + tabW, tabY + tabH, COL_ACCENT_GOLD);
            } else {
                g.fill(tx, tabY + tabH - 1, tx + tabW, tabY + tabH, COL_BORDER);
            }
            int textX = tx + (tabW - textRenderer.getWidth(tabs[i])) / 2;
            g.drawText(textRenderer, tabs[i], textX, tabY + 3, textCol, false);
        }
    }

    // ---- Target Master tab ----

    private int targetsBtnW;
    private int targetsPanelW;
    private int targetsPanelH;
    private int targetsPanelX;
    private int targetsPanelY;
    // HUD drag-preview geometry (computed at build, used by render + mouse handlers)
    private int hudPreviewX, hudPreviewY, hudPreviewW, hudPreviewH;
    private boolean draggingHudPreview = false;

    private void buildTargetsWidgets() {
        TargetMasterConfig cfg = TargetMasterConfig.get();

        targetsPanelX = guiLeft + PANEL_MARGIN;
        targetsPanelY = guiTop + PANEL_MARGIN + 22 + TAB_BAR_HEIGHT;
        targetsPanelW = GUI_WIDTH - PANEL_MARGIN * 2;
        targetsPanelH = GUI_HEIGHT - PANEL_MARGIN * 2 - 22 - TAB_BAR_HEIGHT;
        int btnX = targetsPanelX + 12;
        int btnY = targetsPanelY + 14;
        targetsBtnW = targetsPanelW - 24;
        int rowH = 22;

        addToggleRow(btnX, btnY, targetsBtnW, "Mod Enabled", cfg.enabled, v -> { cfg.enabled = v; TargetMasterConfig.save(); });
        btnY += rowH;

        add(ButtonWidget.builder(
                Text.literal("Marker Particle: §7" + cfg.markerParticle),
                b -> {
                    if (this.client != null) this.client.setScreen(new ParticlePickerScreen(this));
                }).dimensions(btnX, btnY, targetsBtnW, 20).build());
        btnY += rowH;

        addSliderRow(btnX, btnY, targetsBtnW, "Beam Height", cfg.beamHeight, 4f, 32f, 1f,
                v -> { cfg.beamHeight = v; TargetMasterConfig.save(); });
        btnY += rowH;
        addSliderRow(btnX, btnY, targetsBtnW, "Beam Puffs (column)", cfg.beamSteps, 3f, 16f, 1f,
                v -> { cfg.beamSteps = v.intValue(); TargetMasterConfig.save(); });
        btnY += rowH;
        addSliderRow(btnX, btnY, targetsBtnW, "Ring Puffs", cfg.ringCount, 4f, 16f, 1f,
                v -> { cfg.ringCount = v.intValue(); TargetMasterConfig.save(); });
        btnY += rowH;

        addToggleRow(btnX, btnY, targetsBtnW, "Show Health #", cfg.showHealthText, v -> { cfg.showHealthText = v; TargetMasterConfig.save(); });
        btnY += rowH;

        addSliderRow(btnX, btnY, targetsBtnW, "HUD Size", cfg.hudScale, 0.5f, 2.0f, 0.05f,
                v -> { cfg.hudScale = v; TargetMasterConfig.save(); });
        btnY += rowH;

        int halfW = (targetsBtnW - 6) / 2;
        Text msg = Text.literal("World Mark Density");
        add(CyclingButtonWidget.<Integer>builder(
                        i -> Text.literal("World Mark: " + densityLabel(i)),
                        () -> cfg.particleDensity)
                .values(java.util.Arrays.asList(0, 1, 2, 4))
                .build(btnX, btnY, halfW, 20, msg,
                        (b, v) -> { cfg.particleDensity = v; TargetMasterConfig.save(); }));
        Text hudMsg = Text.literal("HUD Layout");
        add(CyclingButtonWidget.<Integer>builder(
                        i -> Text.literal("HUD Layout: " + hudLabel(i)),
                        () -> cfg.hudLayout)
                .values(java.util.Arrays.asList(0, 1, 2))
                .build(btnX + halfW + 6, btnY, halfW, 20, hudMsg,
                        (b, v) -> { cfg.hudLayout = v; TargetMasterConfig.save(); }));
        btnY += rowH;

        addToggleRow(btnX, btnY, halfW, "Target Hostiles", cfg.allowTargetHostiles, v -> { cfg.allowTargetHostiles = v; TargetMasterConfig.save(); });
        addToggleRow(btnX + halfW + 6, btnY, halfW, "Target Passives", cfg.allowTargetPassive, v -> { cfg.allowTargetPassive = v; TargetMasterConfig.save(); });
        btnY += rowH;
        addToggleRow(btnX, btnY, halfW, "Target Players", cfg.allowTargetPlayers, v -> { cfg.allowTargetPlayers = v; TargetMasterConfig.save(); });
        Text resetBtn = Text.literal("Reset HUD");
        add(ButtonWidget.builder(resetBtn, b -> {
                    cfg.hudPreset = 0;
                    cfg.hudX = -1;
                    cfg.hudY = 60;
                    cfg.hudScale = 1.0f;
                    TargetMasterConfig.save();
                }).dimensions(btnX + halfW + 6, btnY, halfW, 20).build());
        btnY += rowH + 4;

        // HUD drag-preview pane lives below the rows. Geometry is stored so
        // render + mouse handlers agree exactly.
        hudPreviewX = btnX;
        hudPreviewY = btnY + 12;
        hudPreviewW = targetsBtnW;
        hudPreviewH = 80;
    }

    private void renderTargetsTab(DrawContext g) {
        renderHudPreview(g);
    }

    private static float hudScale() {
        TargetMasterConfig cfg = TargetMasterConfig.get();
        return Math.max(0.25f, Math.min(3.0f, cfg.hudScale));
    }

    /** Miniature live preview of the real screen with the HUD rect — drag it to move the HUD. */
    private void renderHudPreview(DrawContext g) {
        int sw = this.width;
        int sh = this.height;
        TargetMasterConfig cfg = TargetMasterConfig.get();

        g.drawText(textRenderer, "HUD position — drag the gold box", hudPreviewX, hudPreviewY - 10, COL_TEXT_SECONDARY, false);
        int px = hudPreviewX, py = hudPreviewY, pw = hudPreviewW, ph = hudPreviewH;
        g.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, COL_BORDER);
        g.fill(px, py, px + pw, py + ph, 0xFF08080E);

        // Keep aspect honest so the preview isn't distorted
        float aspect = sw / (float) Math.max(1, sh);
        int vw = pw;
        int vh = (int) (pw / aspect);
        if (vh > ph) { vh = ph; vw = (int) (ph * aspect); }
        int vx = px + (pw - vw) / 2;
        int vy = py + (ph - vh) / 2;
        g.fill(vx, vy, vx + vw, vy + vh, 0xFF101018);

        if (cfg.hudLayout == 0) {
            String off = "HUD is off (HUD Layout)";
            g.drawText(textRenderer, off, vx + (vw - textRenderer.getWidth(off)) / 2, vy + vh / 2 - 4, COL_TEXT_MUTED, false);
            return;
        }

        boolean compact = cfg.hudLayout == 1;
        float scale = hudScale();
        int hw = (int) ((compact ? 140 : 180) * scale);
        int hh = (int) ((compact ? 26 : 44) * scale);
        int[] pos = hudRealPos(sw, sh, hw, hh);
        int mx = vx + (int) (pos[0] / (float) sw * vw);
        int my = vy + (int) (pos[1] / (float) sh * vh);
        int mw = Math.max(4, (int) (hw / (float) sw * vw));
        int mh = Math.max(3, (int) (hh / (float) sh * vh));
        int accent = 0xFF000000 | cfg.glowColor;
        g.fill(mx - 1, my - 1, mx + mw + 1, my + mh + 1, draggingHudPreview ? 0xFFFFFFFF : accent);
        g.fill(mx, my, mx + mw, my + mh, (0x88 << 24) | (cfg.glowColor & 0x00FFFFFF));
    }

    /** Real-screen HUD origin, mirroring TargetHud.render exactly. */
    private int[] hudRealPos(int sw, int sh, int w, int h) {
        TargetMasterConfig cfg = TargetMasterConfig.get();
        int x, y;
        if (cfg.hudPreset == 1) { x = 8; y = 8; }
        else if (cfg.hudPreset == 2) { x = sw - w - 8; y = 8; }
        else if (cfg.hudPreset == 3) { x = sw / 2 - w / 2; y = sh - h - 8; }
        else { x = cfg.hudX >= 0 ? cfg.hudX : sw / 2 - w / 2; y = cfg.hudY; }
        return new int[]{x, y};
    }

    /** Convert a point inside the preview pane to real screen coords (clamped). */
    private void moveHudToPreviewPoint(double mouseX, double mouseY) {
        int sw = this.width;
        int sh = this.height;
        TargetMasterConfig cfg = TargetMasterConfig.get();
        boolean compact = cfg.hudLayout == 1;
        float scale = hudScale();
        int hw = (int) ((compact ? 140 : 180) * scale);
        int hh = (int) ((compact ? 26 : 44) * scale);

        int px = hudPreviewX, py = hudPreviewY, pw = hudPreviewW, ph = hudPreviewH;
        float aspect = sw / (float) Math.max(1, sh);
        int vw = pw;
        int vh = (int) (pw / aspect);
        if (vh > ph) { vh = ph; vw = (int) (ph * aspect); }
        int vx = px + (pw - vw) / 2;
        int vy = py + (ph - vh) / 2;

        double fx = (mouseX - vx) / (double) Math.max(1, vw);
        double fy = (mouseY - vy) / (double) Math.max(1, vh);
        fx = Math.min(1.0, Math.max(0.0, fx));
        fy = Math.min(1.0, Math.max(0.0, fy));
        cfg.hudPreset = 0; // manual placement from now on
        cfg.hudX = (int) Math.min(Math.max(0, sw - hw), fx * sw - hw / 2.0);
        cfg.hudY = (int) Math.min(Math.max(0, sh - hh), fy * sh - hh / 2.0);
    }

    // ---- ParticleTuner tab (preserved functionality) ----

    private int particlesPanelX;
    private int particlesPanelY;
    private int particlesPanelW;
    private int particlesPanelH;
    private int particlesListX;
    private int particlesListY;
    private int particlesListH;
    private int particlesListW;
    private int particlesRightX;
    private int particlesRightW;

    private void buildParticlesWidgets() {
        particlesPanelX = guiLeft + PANEL_MARGIN;
        particlesPanelY = guiTop + PANEL_MARGIN + 22 + TAB_BAR_HEIGHT;
        particlesPanelW = GUI_WIDTH - PANEL_MARGIN * 2;
        particlesPanelH = GUI_HEIGHT - PANEL_MARGIN * 2 - 22 - TAB_BAR_HEIGHT;
        particlesListX = particlesPanelX + 8;
        particlesListY = particlesPanelY + 8;
        particlesListH = particlesPanelH - 56;
        particlesListW = (particlesPanelW - 16) / 3 - 4;
        particlesRightX = particlesListX + particlesListW + 8;
        particlesRightW = particlesPanelW - (particlesRightX - particlesPanelX) - 8;

        this.search = new TextFieldWidget(textRenderer, particlesListX, particlesListY, particlesListW, 16, Text.literal("Search"));
        this.search.setPlaceholder(Text.literal("Search…"));
        this.add(this.search);

        buildParticleSliders();

        // NOTE: the global Done button lives at panel bottom -24, so this row
        // sits one row above it (-48) instead of overlapping it exactly.
        int bottomY = particlesPanelY + particlesPanelH - 48;
        int btnW = (particlesRightW - 8) / 3;
        add(ButtonWidget.builder(Text.literal("Save"), b ->
                        ProfileStorage.saveProfiles(ParticleTunerClient.PROFILES))
                .dimensions(particlesRightX, bottomY, btnW, 20).build());
        add(ButtonWidget.builder(Text.literal("Reset"), b -> {
                    if (selectedParticle != null) {
                        ParticleProfile p = ParticleCatalog.getOrCreate(selectedParticle);
                        p.countMultiplier = 1; p.rangeMultiplier = 1; p.speedMultiplier = 1;
                        p.spreadX = 1; p.spreadY = 1; p.spreadZ = 1; p.gravity = 0;
                        p.sizeMultiplier = 1; p.offsetX = 0; p.offsetY = 0; p.offsetZ = 0;
                        p.lifetimeOverride = 0; p.alphaMultiplier = 1;
                        p.colorR = -1; p.colorG = -1; p.colorB = -1;
                        p.hasCustomTexture = false;
                        TextureStorage.delete(selectedParticle);
                        rebuildParticleSliders();
                    }
                }).dimensions(particlesRightX + (btnW + 4), bottomY, btnW, 20).build());
        add(ButtonWidget.builder(Text.literal("Texture"), b -> {
                    if (selectedParticle != null && this.client != null) {
                        this.client.setScreen(new TextureEditorScreen(this, selectedParticle));
                    }
                }).dimensions(particlesRightX + (btnW + 4) * 2, bottomY, btnW, 20).build());
    }

    /** (Re)build only the 8 tuning sliders. Called from init and on selection/reset. */
    private void buildParticleSliders() {
        int row = particlesListY;
        if (selectedParticle == null) return;
        addFloatSlider("Count",   1, 128, 1, particlesRightX, row, particlesRightW, p -> p.countMultiplier, 1, (p, v) -> { p.countMultiplier = v; }); row += 20;
        addFloatSlider("Speed",   0, 5,    0.05f, particlesRightX, row, particlesRightW, p -> p.speedMultiplier, 1, (p, v) -> { p.speedMultiplier = v; }); row += 20;
        addFloatSlider("Spread",  0, 16,   0.05f, particlesRightX, row, particlesRightW, p -> Math.max(p.rangeMultiplier, (p.spreadX + p.spreadY + p.spreadZ) / 3f), 1, (p, v) -> {
            p.rangeMultiplier = v; p.spreadX = v; p.spreadY = v; p.spreadZ = v;
        }); row += 20;
        addFloatSlider("Gravity", -1, 1,    0.01f, particlesRightX, row, particlesRightW, p -> p.gravity, 0, (p, v) -> { p.gravity = v; }); row += 20;
        addFloatSlider("Size", 0.01f, 8, 0.05f, particlesRightX, row, particlesRightW, p -> p.sizeMultiplier, 1, (p, v) -> { p.sizeMultiplier = v; }); row += 20;
        addFloatSlider("Offset X", -2, 2, 0.05f, particlesRightX, row, particlesRightW, p -> p.offsetX, 0, (p, v) -> { p.offsetX = v; }); row += 20;
        addFloatSlider("Offset Y", -2, 2, 0.05f, particlesRightX, row, particlesRightW, p -> p.offsetY, 0, (p, v) -> { p.offsetY = v; }); row += 20;
        addFloatSlider("Offset Z", -2, 2, 0.05f, particlesRightX, row, particlesRightW, p -> p.offsetZ, 0, (p, v) -> { p.offsetZ = v; }); row += 20;
    }

    private void rebuildParticleSliders() {
        for (net.minecraft.client.gui.widget.ClickableWidget w : particleSliderWidgets) this.remove(w);
        particlesWidgets.removeAll(particleSliderWidgets);
        particleSliderWidgets.clear();
        // addFloatSlider tracks into buildList; point it at the particles tab
        java.util.List<net.minecraft.client.gui.widget.ClickableWidget> prev = buildList;
        buildList = particlesWidgets;
        buildParticleSliders();
        buildList = prev;
    }

    private void renderParticlesTab(DrawContext g) {
        if (selectedParticle != null) {
            String idStr = selectedParticle.getNamespace() + ":" + selectedParticle.getPath();
            g.drawText(textRenderer, "Selected: " + idStr, particlesListX + particlesListW + 8 + 4, particlesPanelY - 18, COL_ACCENT_CYAN, true);
        } else {
            g.drawText(textRenderer, "Pick a particle", particlesListX + particlesListW + 8 + 4, particlesPanelY - 18, COL_TEXT_MUTED, false);
            g.drawText(textRenderer, "Pick a particle on the left to tune", particlesRightX, particlesListY + 28, COL_TEXT_MUTED, false);
        }

        g.fill(particlesListX - 2, particlesListY + 18, particlesListX + particlesListW + 2, particlesListY + particlesListH + 20, 0xFF202028);
        g.enableScissor(particlesListX, particlesListY + 20, particlesListX + particlesListW, particlesListY + particlesListH + 18);
        int y = particlesListY + 20 - particleListScroll;
        String query = search != null ? search.getText().toLowerCase() : "";
        for (Map.Entry<String, List<Identifier>> ns : ParticleCatalog.groupedByNamespace().entrySet()) {
            g.drawText(textRenderer, "── " + ns.getKey() + " ──", particlesListX + 4, y, COL_TEXT_MUTED, false);
            y += 14;
            for (Identifier id : ns.getValue()) {
                if (!query.isEmpty() && !id.toString().toLowerCase().contains(query)) continue;
                boolean sel = id.equals(selectedParticle);
                if (sel) {
                    g.fill(particlesListX, y - 1, particlesListX + particlesListW, y + 11, COL_SELECTED);
                    g.fill(particlesListX, y - 1, particlesListX + 3, y + 11, COL_ACCENT_GOLD);
                }
                if (y + 12 > particlesListY + 20 && y < particlesListY + particlesListH + 18) {
                    g.drawText(textRenderer, id.getPath(), particlesListX + 4, y, sel ? COL_ACCENT_GOLD : 0xFFFFFFFF, false);
                }
                y += 12;
            }
        }
        g.disableScissor();
    }

    public static final String DISCORD_URL = "https://discord.gg/ghc2Uu8Wzd";

    private void addDoneButton() {
        int btnX = guiLeft + PANEL_MARGIN + 12;
        int btnY = guiTop + GUI_HEIGHT - PANEL_MARGIN - 24;
        int btnW = GUI_WIDTH - PANEL_MARGIN * 2 - 24;
        int halfW = (btnW - 6) / 2;
        if (doneButton != null) this.remove(doneButton);
        doneButton = ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(btnX, btnY, halfW, 20).build();
        addDrawableChild(doneButton);
        addDrawableChild(ButtonWidget.builder(Text.literal("✦ Discord"), b -> openDiscord())
                .dimensions(btnX + halfW + 6, btnY, halfW, 20).build());
    }

    private void openDiscord() {
        try {
            net.minecraft.util.Util.getOperatingSystem().open(new java.net.URI(DISCORD_URL));
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("§bOpening Discord invite…"), false);
            }
        } catch (Exception e) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("§cCould not open browser. Join here: " + DISCORD_URL), false);
            }
        }
    }

    private void addToggleRow(int x, int y, int w, String label, boolean current,
                              java.util.function.Consumer<Boolean> onChange) {
        Text onText = Text.literal(label + "  §a[ON]");
        Text offText = Text.literal(label + "  §c[OFF]");
        add(CyclingButtonWidget.onOffBuilder(onText, offText, current)
                .build(x, y, w, 20, Text.literal(label), (btn, v) -> onChange.accept(v)));
    }

    private void addSliderRow(int x, int y, int w, String label, float current, float min, float max, float step,
                              java.util.function.Consumer<Float> onChange) {
        // Native IntSliderExt for step=1 integers, FloatSliderExt otherwise
        if (step >= 1f && Math.floor(step) == step) {
            add(com.targetmaster.IntSliderExt.builder(label, (int) min, (int) max, (int) step)
                    .bounds(x, y, w, 20)
                    .setValue((int) current)
                    .onChange(v -> onChange.accept((float) v))
                    .build());
        } else {
            add(com.targetmaster.FloatSliderExt.builder(label, min, max, step)
                    .bounds(x, y, w, 20)
                    .setValue(current)
                    .onChange(v -> onChange.accept(v))
                    .build());
        }
    }

    private void addFloatSlider(String label, float min, float max, float step,
                                int x, int y, int width,
                                java.util.function.ToDoubleFunction<ParticleProfile> getter, float def,
                                java.util.function.BiConsumer<ParticleProfile, Float> setter) {
        ParticleProfile profile = currentProfile();
        float val = profile == null ? def : (float) getter.applyAsDouble(profile);
        com.targetmaster.FloatSliderExt s = com.targetmaster.FloatSliderExt.builder(label, min, max, step)
                .bounds(x, y, width, 20)
                .setValue(val)
                .onChange(v -> {
                    ParticleProfile p = ensureProfile();
                    if (p != null) {
                        setter.accept(p, (Float) v);
                        ProfileStorage.saveProfiles(ParticleTunerClient.PROFILES);
                    }
                })
                .build();
        add(s);
        particleSliderWidgets.add(s);
    }

    private long lastSaveMs = 0L;
    private void autosave() {
        long now = System.currentTimeMillis();
        if (lastSaveMs > 0L && now - lastSaveMs < 250L) return;
        lastSaveMs = now;
        ProfileStorage.saveProfiles(ParticleTunerClient.PROFILES);
    }

    private ParticleProfile ensureProfile() {
        if (selectedParticle == null) {
            selectedParticle = Registries.PARTICLE_TYPE.getIds().stream().findFirst().orElse(null);
        }
        return ParticleCatalog.getOrCreate(selectedParticle);
    }

    private ParticleProfile currentProfile() {
        return selectedParticle == null ? null : ParticleTunerClient.PROFILES.get(selectedParticle);
    }

    // ---- rendering ----

    private void drawRoundedRect(DrawContext g, int x, int y, int w, int h, int corner, int color) {
        g.fill(x + corner, y, x + w - corner, y + h, color);
        g.fill(x, y + corner, x + w, y + h - corner, color);
        g.fill(x + corner, y + corner, x + w - corner, y + h - corner, color);
    }

    private void drawGlowRect(DrawContext g, int x, int y, int w, int h, int corner, int color) {
        int a = (color >>> 24) & 0xFF;
        int r = (color >>> 16) & 0xFF;
        int gg = (color >>> 8) & 0xFF;
        int b = color & 0xFF;
        int outer = (a / 2 << 24) | (r << 16) | (gg << 8) | b;
        drawRoundedRect(g, x - 1, y - 1, w + 2, h + 2, corner + 1, outer);
        drawRoundedRect(g, x, y, w, h, corner, color);
    }

    private void drawCornerAccents(DrawContext g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + CORNER * 2, y + 1, color);
        g.fill(x, y, x + 1, y + CORNER * 2, color);
        g.fill(x + w - CORNER * 2, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + CORNER * 2, color);
        g.fill(x, y + h - 1, x + CORNER * 2, y + h, color);
        g.fill(x, y + h - CORNER * 2, x + 1, y + h, color);
        g.fill(x + w - CORNER * 2, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - CORNER * 2, x + w, y + h, color);
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, this.width, this.height, 0xCC05050A);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        renderFrame(g);
        renderTabBar(g);
        if (currentTab == TAB_PARTICLES) renderParticlesTab(g);
        else renderTargetsTab(g);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderFrame(DrawContext g) {
        int fx = guiLeft;
        int fy = guiTop;
        int fw = GUI_WIDTH;
        int fh = GUI_HEIGHT;

        drawGlowRect(g, fx, fy, fw, fh, CORNER, COL_BG_DARK);
        drawRoundedRect(g, fx, fy, fw, fh, CORNER, COL_BG_DARK);
        g.fill(fx, fy, fx + fw, fy + 1, COL_BORDER_LIGHT);
        g.fill(fx, fy + fh - 1, fx + fw, fy + fh, COL_BORDER);
        drawCornerAccents(g, fx, fy, fw, fh, COL_ACCENT_GOLD);

        String title = currentTab == TAB_TARGETS ? "✦ TargetMaster ✦" : "✦ ParticleTuner ✦";
        int tw = textRenderer.getWidth(title);
        g.drawText(textRenderer, title, fx + (fw - tw) / 2, fy + 4, COL_ACCENT_GOLD, true);

        g.fill(fx + 1, fy + 18, fx + fw - 1, fy + 19, COL_BORDER_LIGHT);
        int segW = (fw - 2) / 4;
        long t = System.currentTimeMillis() / 50L;
        for (int i = 0; i < 4; i++) {
            int sx = fx + 1 + i * segW + segW / 2 - 1;
            int alpha = (int) (120 + 100 * Math.sin(t * 0.1 + i));
            int col = (Math.min(255, alpha) << 24) | 0xFFD700;
            g.fill(sx, fy + 18, sx + 2, fy + 19, col);
        }

        int panelX = fx + PANEL_MARGIN;
        int panelY = fy + PANEL_MARGIN + 22 + TAB_BAR_HEIGHT;
        int panelW = fw - PANEL_MARGIN * 2;
        int panelH = fh - PANEL_MARGIN * 2 - 22 - TAB_BAR_HEIGHT;
        drawRoundedRect(g, panelX, panelY, panelW, panelH, CORNER, COL_BG_PANEL);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, COL_BORDER_LIGHT);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, COL_BORDER);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, COL_BORDER_LIGHT);
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, COL_BORDER);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        double mx = click.x();
        double my = click.y();

        // Tab strip — painted labels, clicks handled by coordinates (no overlay buttons)
        if (my >= tabStripY0() && my < tabStripY1()) {
            int tabW = tabStripW();
            if (mx >= guiLeft + 1 && mx < guiLeft + 1 + tabW) { switchTab(TAB_TARGETS); return true; }
            if (mx >= guiLeft + 1 + tabW && mx < guiLeft + 1 + tabW * 2) { switchTab(TAB_PARTICLES); return true; }
        }

        // HUD drag-preview (targets tab only)
        if (currentTab == TAB_TARGETS && TargetMasterConfig.get().hudLayout != 0
                && mx >= hudPreviewX && mx < hudPreviewX + hudPreviewW
                && my >= hudPreviewY && my < hudPreviewY + hudPreviewH) {
            draggingHudPreview = true;
            moveHudToPreviewPoint(mx, my);
            return true;
        }

        // Particle list click (only when on particles tab)
        if (currentTab == TAB_PARTICLES && search != null) {
            if (mx >= particlesListX && mx < particlesListX + particlesListW
                    && my >= particlesListY + 20 && my < particlesListY + particlesListH + 18) {
                int y = particlesListY + 20 - particleListScroll;
                String query = search.getText().toLowerCase();
                for (Map.Entry<String, List<Identifier>> ns : ParticleCatalog.groupedByNamespace().entrySet()) {
                    y += 14;
                    for (Identifier id : ns.getValue()) {
                        if (!query.isEmpty() && !id.toString().toLowerCase().contains(query)) continue;
                        if (my >= y && my < y + 12) {
                            selectedParticle = id;
                            rebuildParticleSliders();
                            return true;
                        }
                        y += 12;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (super.mouseDragged(click, deltaX, deltaY)) return true;
        if (draggingHudPreview && currentTab == TAB_TARGETS) {
            moveHudToPreviewPoint(click.x(), click.y());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingHudPreview) {
            draggingHudPreview = false;
            TargetMasterConfig.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab == TAB_PARTICLES) {
            if (mouseX >= particlesListX && mouseX < particlesListX + particlesListW
                    && mouseY >= particlesListY + 20 && mouseY < particlesListY + particlesListH + 18) {
                particleListScroll = Math.max(0, (int) (particleListScroll - verticalAmount * 12));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        TargetMasterConfig.save();
        ProfileStorage.saveProfiles(ParticleTunerClient.PROFILES);
        super.close();
    }

    private static String densityLabel(int v) {
        return switch (v) {
            case 0 -> "Off";
            case 1 -> "Strong (every tick)";
            case 2 -> "Normal (every 2 ticks)";
            case 4 -> "Subtle (every 4 ticks)";
            default -> "Every " + v + " ticks";
        };
    }

    private static String hudLabel(int v) {
        return switch (v) {
            case 0 -> "Off";
            case 1 -> "Compact";
            case 2 -> "Full";
            default -> "Full";
        };
    }
}
