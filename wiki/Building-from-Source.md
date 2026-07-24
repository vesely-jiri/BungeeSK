# Building from Source

## Requirements

- **JDK 21**
- The Gradle wrapper (`./gradlew`) — no separate Gradle install needed

## Build

```bash
./gradlew buildAll
```

The distributable jar is:

| Jar | Path | Runs on |
|-----|------|---------|
| **`BungeeSK.jar`** | `build/libs/` | Paper/Spigot + BungeeCord + Velocity |

It is fused from two **internal** per-platform jars (build intermediates, not installed):

| Internal jar | Path |
|--------------|------|
| `BungeeSK-Paper-Bungee.jar` | `BungeeSK/build/libs/` |
| `BungeeSK-Velocity.jar` | `VelocitySK/build/libs/` |

## How the universal jar is built

A single re-shade can't satisfy both proxies at once: BungeeCord needs Adventure **bundled** (relocated to `fr.zorg.shaded.kyori`) while Velocity provides its own `net.kyori` and must **not** have it bundled. So `:universalJar` **fuses** the two already-shaded jars — the BungeeCord/Paper jar as the base plus only the Velocity classes — so each side keeps the references it was compiled with. The three plugin descriptors (`plugin.yml`, `bungee.yml`, `velocity-plugin.json`) coexist and each platform loads only its own.

## Windows / OneDrive note

If the project lives inside a OneDrive-synced folder, the sync client can hold file handles open on `build/` and make Gradle fail with *“Unable to delete directory”*. Pass a non-synced build directory:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\gradlew.bat buildAll -PbuildDirBase=C:/Users/you/AppData/Local/bungeesk-build
```

CI leaves `-PbuildDirBase` unset and uses the default `build/`.

## Jar size

The jars are kept small by shipping SQLite native libraries only for the platforms a Minecraft server realistically runs on (Linux glibc + Alpine/musl and Windows on x86_64/aarch64, plus Apple-Silicon Mac). If you need another platform, add it back by editing `ext.prunedJarPaths` in the root `build.gradle`.
