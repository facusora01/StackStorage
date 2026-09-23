# StackStorage

Minecraft mod for Forge and NeoForge. Each Minecraft version lives on its own branch
(`1.20.1`, `1.20.4`, ... `26.2`).

This branch: Minecraft 1.20.1, built on Forge 47.4.10. The same jar also runs on
NeoForge 1.20.1 (47.1.x).

## Layout

- `common/` — the mod itself, vanilla Minecraft code only. Compiled into every loader's jar.
- `forge/` — Forge entry point, events, networking and config, handing off to `common/`.

Branches for 1.20.2 and later also have a `neoforge/` module next to `forge/`.

## Development setup

```
./gradlew :forge:genEclipseRuns   # or genIntellijRuns / genVSCodeRuns
./gradlew :forge:build            # -> forge/build/libs/stackstorage-forge-1.20.1-<version>.jar
./gradlew :forge:runClient
./gradlew :forge:runGameTestServer
```

Mod id: `stackstorage`
Base package: `com.facusora.stackstorage`

See `README.txt` for the original Forge MDK build instructions.
