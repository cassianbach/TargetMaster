package com.targetmaster;

import com.targetmaster.config.TargetMasterConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParticleTunerClient implements ClientModInitializer {
    public static final Map<Identifier, ParticleProfile> PROFILES = new LinkedHashMap<>();
    public static ProfileStorage.Config CONFIG = new ProfileStorage.Config();

    // Target master keybindings (G/H) are owned by TargetInput — defined in TargetInput.java.
    public static final KeyBinding OPEN_EDITOR_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.particletuner.open_editor", GLFW.GLFW_KEY_P, KeyBinding.Category.MISC));

    @Override
    public void onInitializeClient() {
        PROFILES.putAll(ProfileStorage.loadProfiles());
        CONFIG = ProfileStorage.loadConfig();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.runDirectory != null) {
            TextureStorage.setRunDir(mc.runDirectory.toPath());
        }
        TextureStorage.migrateLegacyFiles();
        TextureStorage.ensureEnabled();

        // Load target master config + wire HUD/input/manager
        TargetMasterConfig.load();

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureStorage.tickDeferredReload());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Unified menu (has Particles tab)
            if (OPEN_EDITOR_KEY.wasPressed()) {
                MinecraftClient.getInstance().setScreen(new com.targetmaster.screen.TargetMasterScreen());
            }
        });

        // Wire target master into the same tick pipeline
        TargetInput.init();
        TargetHud.init();
        TargetManager.init();

        ParticleTuner.LOGGER.info("ParticleTuner initialized ({} profiles loaded)", PROFILES.size());
    }
}
