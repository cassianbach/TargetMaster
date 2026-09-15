package com.targetmaster;

import com.targetmaster.ParticleProfile;
import com.targetmaster.ParticleTunerClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class ParticleCatalog {
    private ParticleCatalog() {
    }

    public static List<Identifier> allParticleIds() {
        ArrayList<Identifier> ids = new ArrayList<Identifier>();
        for (ParticleType type : Registries.PARTICLE_TYPE) {
            Identifier id = Registries.PARTICLE_TYPE.getId(type);
            if (id == null) continue;
            ids.add(id);
        }
        return ids;
    }

    public static Map<String, List<Identifier>> groupedByNamespace() {
        LinkedHashMap<String, List<Identifier>> map = new LinkedHashMap<String, List<Identifier>>();
        for (ParticleType type : Registries.PARTICLE_TYPE) {
            Identifier id = Registries.PARTICLE_TYPE.getId(type);
            if (id == null) continue;
            map.computeIfAbsent(id.getNamespace(), k -> new ArrayList()).add(id);
        }
        return map;
    }

    public static ParticleProfile getOrCreate(Identifier id) {
        return ParticleTunerClient.PROFILES.computeIfAbsent(id, ParticleProfile::new);
    }
}
