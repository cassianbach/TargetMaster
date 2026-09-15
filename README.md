# TargetMaster (Fabric 1.21.11)

Client-side Fabric mod for Minecraft 1.21.11: right-click any entity to target it —
it gets a tall in-world beam + ground ring in your marker particle, plus an on-screen
HUD panel with name, type, health bar and distance. Also bundles the ParticleTuner
in-game particle editor (tune count/speed/spread/gravity/size/offset/color/alpha per
particle, repixel textures, per-particle profiles).

## Controls

- **Right-click** an entity — target it (right-click again to untarget)
- **G** — unified menu (Targets / Particles tabs)
- **H** — clear target
- **P** — unified menu (same as G)
- **Drag the HUD panel** in-game, or drag the gold box in the menu's HUD preview

## Menu (G)

Targets tab: mod toggle, marker-particle picker (any vanilla particle),
beam height / puffs / ring size sliders, HUD size slider, HUD layout
(Off / Compact / Full), world-mark density, target filters, HUD position
preview with drag-to-move.

Particles tab: searchable particle list, 8 tuning sliders
(count/speed/spread/gravity/size/offset), Save / Reset / Texture (repixel editor).

Join the Discord from the **✦ Discord** button at the bottom of the menu.

## Build

```
./gradlew build
```

Jar lands in `build/libs/`. Requires Fabric Loader ≥ 0.18.1 and Fabric API.
Java 21.
