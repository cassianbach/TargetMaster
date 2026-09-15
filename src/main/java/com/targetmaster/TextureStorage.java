package com.targetmaster;

import com.targetmaster.ParticleProfile;
import com.targetmaster.ParticleTuner;
import com.targetmaster.ParticleTunerClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.util.Identifier;

public final class TextureStorage {
    private static final String PACK_ID = "particletuner_custom";
    private static final Map<Identifier, int[]> pixelBuffers = new HashMap<Identifier, int[]>();
    private static Path packRootOverride = null;
    private static boolean pendingReload = false;

    private TextureStorage() {
    }

    public static void setRunDir(Path runDir) {
        packRootOverride = runDir.resolve("resourcepacks").resolve(PACK_ID);
    }

    public static void migrateLegacyFiles() {
        Path legacy = Path.of(System.getProperty("user.dir"), "particletuner", "textures");
        if (!Files.isDirectory(legacy, new LinkOption[0])) {
            return;
        }
        try (Stream<Path> stream = Files.walk(legacy, new FileVisitOption[0]);){
            stream.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).filter(p -> p.toString().endsWith(".png")).forEach(p -> {
                Identifier pid;
                Path dest;
                Path rel = legacy.relativize((Path)p);
                if (rel.getNameCount() < 2) {
                    return;
                }
                String namespace = rel.getName(0).toString();
                String nameNoExt = rel.subpath(1, rel.getNameCount()).toString();
                if (nameNoExt.endsWith(".png")) {
                    nameNoExt = nameNoExt.substring(0, nameNoExt.length() - 4);
                }
                if ((dest = TextureStorage.fileFor(pid = Identifier.of((String)namespace, (String)nameNoExt))) != null && !Files.exists(dest, new LinkOption[0])) {
                    try {
                        Files.createDirectories(dest.getParent(), new FileAttribute[0]);
                        Files.copy(p, dest, new CopyOption[0]);
                        ParticleTuner.LOGGER.info("Migrated legacy texture {} -> {}", p, (Object)dest);
                    } catch (IOException e) {
                        ParticleTuner.LOGGER.warn("Failed to migrate legacy texture {}", p, (Object)e);
                    }
                }
            });
        } catch (IOException e) {
            ParticleTuner.LOGGER.warn("Failed to migrate legacy textures", (Throwable)e);
        }
    }

    private static Path packRoot() {
        if (packRootOverride != null) {
            return packRootOverride;
        }
        return Path.of(System.getProperty("user.dir"), "resourcepacks", PACK_ID);
    }

    public static Path fileFor(Identifier particleId) {
        List<Path> sprites = TextureStorage.spriteFiles(particleId);
        return sprites.isEmpty() ? null : sprites.get(0);
    }

    private static Path pathForSprite(String spriteId) {
        int colon = spriteId.indexOf(58);
        String ns = colon >= 0 ? spriteId.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? spriteId.substring(colon + 1) : spriteId;
        return TextureStorage.packRoot().resolve("assets").resolve(ns).resolve("textures").resolve("particle").resolve(path + ".png");
    }

    public static List<Path> spriteFiles(Identifier particleId) {
        ArrayList<Path> result = new ArrayList<Path>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getResourceManager() != null) {
            Identifier jsonId = Identifier.of((String)particleId.getNamespace(), (String)("particles/" + particleId.getPath() + ".json"));
            Optional res = mc.getResourceManager().getResource(jsonId);
            if (res.isPresent()) {
                try (BufferedReader reader = ((Resource)res.get()).getReader();){
                    JsonObject obj = JsonParser.parseReader((Reader)reader).getAsJsonObject();
                    if (obj.has("textures")) {
                        JsonArray arr = obj.getAsJsonArray("textures");
                        for (JsonElement e : arr) {
                            String spriteId = e.getAsString();
                            result.add(TextureStorage.pathForSprite(spriteId));
                        }
                    }
                } catch (IOException | IllegalStateException e) {
                    ParticleTuner.LOGGER.warn("Failed to parse sprite list for {}", (Object)particleId, (Object)e);
                }
            }
        }
        return result;
    }

    public static boolean isRepixelable(Identifier particleId) {
        return !TextureStorage.spriteFiles(particleId).isEmpty();
    }

    public static boolean exists(Identifier particleId) {
        for (Path p : TextureStorage.spriteFiles(particleId)) {
            if (!Files.exists(p, new LinkOption[0])) continue;
            return true;
        }
        return false;
    }

    public static void delete(Identifier particleId) {
        for (Path p : TextureStorage.spriteFiles(particleId)) {
            try {
                Files.deleteIfExists(p);
            } catch (IOException iOException) {}
        }
        ParticleProfile p = ParticleTunerClient.PROFILES.get(particleId);
        if (p != null) {
            p.hasCustomTexture = false;
        }
        TextureStorage.applyAndReload();
    }

    public static int[] loadOrCreateBuffer(Identifier id, int size) {
        return pixelBuffers.computeIfAbsent(id, k -> new int[size * size]);
    }

    public static void save(Identifier particleId, int size, int[] pixels) {
        int[] stored;
        boolean anyWritten = false;
        for (Path path : TextureStorage.spriteFiles(particleId)) {
            try {
                Files.createDirectories(path.getParent(), new FileAttribute[0]);
                NativeImage img = new NativeImage(size, size, false);
                for (int y = 0; y < size; ++y) {
                    for (int x = 0; x < size; ++x) {
                        img.setColorArgb(x, y, pixels[y * size + x]);
                    }
                }
                img.writeTo(path);
                img.close();
                anyWritten = true;
            } catch (IOException e) {
                ParticleTuner.LOGGER.warn("Failed to save texture for {}", (Object)particleId, (Object)e);
            }
        }
        if (!anyWritten) {
            return;
        }
        ParticleProfile p = ParticleTunerClient.PROFILES.get(particleId);
        if (p != null) {
            p.hasCustomTexture = true;
        }
        if ((stored = pixelBuffers.get(particleId)) != null && stored.length == pixels.length) {
            System.arraycopy(pixels, 0, stored, 0, pixels.length);
        } else {
            pixelBuffers.put(particleId, (int[])pixels.clone());
        }
        TextureStorage.applyAndReload();
    }

    private static void writePackMeta() {
        String json = "{\"pack\":{\"description\":\"ParticleTuner custom textures\",\"min_format\":75,\"max_format\":75}}";
        Path f = TextureStorage.packRoot().resolve("pack.mcmeta");
        try {
            Files.createDirectories(f.getParent(), new FileAttribute[0]);
            Files.write(f, json.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
        } catch (IOException e) {
            ParticleTuner.LOGGER.warn("Failed to write pack.mcmeta", (Throwable)e);
        }
    }

    public static void ensureEnabled() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        TextureStorage.writePackMeta();
        try {
            ResourcePackManager rpm = mc.getResourcePackManager();
            rpm.scanPacks();
            String profileId = null;
            for (String id : rpm.getIds()) {
                if (!id.endsWith(PACK_ID)) continue;
                profileId = id;
                break;
            }
            if (profileId != null) {
                if (!rpm.getEnabledIds().contains(profileId)) {
                    rpm.enable(profileId);
                    pendingReload = true;
                }
            } else {
                ParticleTuner.LOGGER.warn("ParticleTuner custom texture pack '{}' not found after scan; root={}", (Object)PACK_ID, (Object)TextureStorage.packRoot());
            }
        } catch (Exception e) {
            ParticleTuner.LOGGER.warn("Failed to enable ParticleTuner pack", (Throwable)e);
        }
    }

    public static void tickDeferredReload() {
        if (!pendingReload) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getResourceManager() == null) {
            return;
        }
        pendingReload = false;
        TextureStorage.reloadResources();
    }

    public static void reloadResources() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return;
        }
        try {
            mc.reloadResources();
        } catch (Exception e) {
            ParticleTuner.LOGGER.warn("Failed to reload resources", (Throwable)e);
        }
    }

    public static void applyAndReload() {
        TextureStorage.ensureEnabled();
        TextureStorage.reloadResources();
    }

    public static void clear() {
        pixelBuffers.clear();
    }
}
