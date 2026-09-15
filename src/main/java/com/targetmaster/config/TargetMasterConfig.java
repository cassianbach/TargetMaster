package com.targetmaster.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TargetMasterConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean enabled = true;
    public boolean showName = true;
    public boolean showHealthBar = true;
    public boolean showHealthText = true;
    public int glowColor = 0xFFFFD700;
    public boolean allowTargetPlayers = true;
    public boolean allowTargetHostiles = true;
    public boolean allowTargetPassive = true;
    public boolean requireSneakToClear = false;
    public int hudX = -1;
    public int hudY = 60;
    public float hudScale = 1.0f;
    public int particleDensity = 2;
    public int hudLayout = 2;
    public int hudPreset = 0;
    // Particle used for the target beam / ground ring. Default = minecraft:dust (colored).
    // Can be changed via the Targets tab in the unified menu.
    public String markerParticle = "minecraft:dust";
    // Beam height in blocks (vertical reach of the in-world mark)
    public float beamHeight = 18.0f;
    // Beam thickness (how many dust puffs per layer)
    public int beamSteps = 7;
    // Ground ring particle count
    public int ringCount = 8;

    private static TargetMasterConfig INSTANCE;

    public static TargetMasterConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new TargetMasterConfig();
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        INSTANCE = new TargetMasterConfig();
        Path file = configPath();
        if (Files.exists(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                TargetMasterConfig loaded = GSON.fromJson(reader, TargetMasterConfig.class);
                if (loaded != null) {
                    INSTANCE = loaded;
                }
            } catch (IOException e) {
                com.targetmaster.ParticleTuner.LOGGER.warn("Failed to load TargetMaster config: {}", e.getMessage());
            }
        }
    }

    public static void save() {
        Path file = configPath();
        try {
            Files.createDirectories(file.getParent());
            try (var writer = Files.newBufferedWriter(file)) {
                GSON.toJson(get(), writer);
            }
        } catch (IOException e) {
            com.targetmaster.ParticleTuner.LOGGER.warn("Failed to save TargetMaster config: {}", e.getMessage());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("targetmaster.json");
    }

    public static Entity getTargetedEntity() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null) return null;
        int id = com.targetmaster.TargetManager.getTargetId();
        if (id < 0) return null;
        Entity e = mc.world.getEntityById(id);
        if (e == null) return null;
        if (!e.isAlive()) {
            com.targetmaster.TargetManager.clear();
            return null;
        }
        double maxDist = 64.0;
        if (mc.player.distanceTo(e) > maxDist) {
            return null;
        }
        return e;
    }

    public static boolean isTargetable(Entity entity) {
        if (entity == null) return false;
        TargetMasterConfig cfg = get();
        if (entity.isSpectator()) return false;
        if (entity instanceof PlayerEntity && !cfg.allowTargetPlayers) return false;
        SpawnGroup cat = entity.getType().getSpawnGroup();
        if (cat == SpawnGroup.MONSTER && !cfg.allowTargetHostiles) return false;
        boolean isPassive = cat == SpawnGroup.CREATURE
                || cat == SpawnGroup.AMBIENT
                || cat == SpawnGroup.WATER_CREATURE
                || cat == SpawnGroup.WATER_AMBIENT
                || cat == SpawnGroup.AXOLOTLS;
        if (isPassive && !cfg.allowTargetPassive) return false;
        return true;
    }
}
