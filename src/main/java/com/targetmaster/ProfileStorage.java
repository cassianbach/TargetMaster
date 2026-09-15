package com.targetmaster;

import com.targetmaster.ParticleProfile;
import com.targetmaster.ParticleTuner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.Identifier;

public final class ProfileStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PROFILES_FILE = ProfileStorage.configDir().resolve("profiles.json");
    private static final Path CONFIG_FILE = ProfileStorage.configDir().resolve("config.json");

    private static Path configDir() {
        return Path.of(System.getProperty("user.dir"), "config", "particletuner");
    }

    private ProfileStorage() {
    }

    public static Map<Identifier, ParticleProfile> loadProfiles() {
        LinkedHashMap<Identifier, ParticleProfile> map = new LinkedHashMap<Identifier, ParticleProfile>();
        if (!Files.exists(PROFILES_FILE, new LinkOption[0])) {
            return map;
        }
        try (BufferedReader reader = Files.newBufferedReader(PROFILES_FILE);){
            JsonObject root = JsonParser.parseReader((Reader)reader).getAsJsonObject();
            JsonObject entries = root.getAsJsonObject("profiles");
            if (entries == null) {
                LinkedHashMap<Identifier, ParticleProfile> linkedHashMap = map;
                return linkedHashMap;
            }
            Type type = new TypeToken<ParticleProfile>(){}.getType();
            Iterator iterator = entries.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                Identifier id = Identifier.of((String)((String)entry.getKey()));
                ParticleProfile profile = (ParticleProfile)GSON.fromJson((JsonElement)entry.getValue(), type);
                if (profile == null) continue;
                profile.particleId = id;
                map.put(id, profile);
            }
            return map;
        } catch (IOException e) {
            ParticleTuner.LOGGER.warn("Failed to load profiles", (Throwable)e);
        }
        return map;
    }

    public static void saveProfiles(Map<Identifier, ParticleProfile> profiles) {
        try {
            Files.createDirectories(PROFILES_FILE.getParent(), new FileAttribute[0]);
            JsonObject root = new JsonObject();
            JsonObject entries = new JsonObject();
            for (Map.Entry<Identifier, ParticleProfile> entry : profiles.entrySet()) {
                entries.add(entry.getKey().toString(), GSON.toJsonTree((Object)entry.getValue()));
            }
            root.add("profiles", (JsonElement)entries);
            try (BufferedWriter writer = Files.newBufferedWriter(PROFILES_FILE, new OpenOption[0]);){
                GSON.toJson((JsonElement)root, (Appendable)writer);
            }
        } catch (IOException e) {
            ParticleTuner.LOGGER.warn("Failed to save profiles", (Throwable)e);
        }
    }

    public static Config loadConfig() {
        if (!Files.exists(CONFIG_FILE, new LinkOption[0])) {
            return new Config();
        }
        try (BufferedReader reader = Files.newBufferedReader(CONFIG_FILE)) {
            return GSON.fromJson(reader, Config.class);
        } catch (IOException e) {
            ParticleTuner.LOGGER.warn("Failed to load config", e);
            return new Config();
        }
    }

    public static void saveConfig(Config config) {
        try {
            Files.createDirectories(CONFIG_FILE.getParent(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_FILE, new OpenOption[0]);){
                GSON.toJson((Object)config, (Appendable)writer);
            }
        } catch (IOException e) {
            ParticleTuner.LOGGER.warn("Failed to save config", (Throwable)e);
        }
    }

    public static class Config {
        public boolean escapeButtonVisible = true;
        public boolean titleButtonVisible = true;
        public int defaultTextureSize = 16;
        public String activeProfileName = "default";
    }
}
