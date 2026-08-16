# Project Expansion: Arcanum

**A maintained fork of Project Expansion for Minecraft 1.20.1.**

Arcanum is an independent maintenance fork of [Project Expansion](https://github.com/DonovanDMC/ProjectExpansion), originally created by **Donovan_DMC**. The first Arcanum release is limited to maintenance, compatibility, engineering cleanup, and verified bug fixes. It does not add new blocks, items, machines, recipes, mechanics, GUI features, or gameplay.

Current maintainer identity: **to be confirmed before the first release**.

## Compatibility and migration

Arcanum uses the independent runtime mod ID and registry/resource namespace `projectexa` to avoid collisions with other translations and forks. Existing official Project Expansion `1.1.3` worlds use the legacy `projectexpansion` namespace; Forge missing-mapping handlers plus a block-entity NBT migration hook remap matching blocks, items, block entities, menus, enchantments, and sounds to the new namespace during load.

Before migrating, make a backup and stop Minecraft:

1. Remove the official `projectexpansion-1.20.1-1.1.3.jar`.
2. Install the Arcanum JAR.
3. Do not keep both JARs installed at the same time.
4. Load the backed-up world and verify machines, items, EMC, player data, and configuration. The migrated content will be registered under `projectexa` after the remap.

The migration and runtime checks are release-gate tests. Arcanum is not affiliated with or officially supported by ProjectE, and ProjectE issues should be reported to the ProjectE project rather than here.

## Supported environment

- Minecraft `1.20.1`
- Forge `1.20.1`
- ProjectE `PE1.0.1` (`1.20.1`)
- Java 17

Optional integrations remain optional, including Curios, JEI, Jade, WTHIT, The One Probe, AE2, and Pipez.

## Development

Build from a clean checkout with Java 17:

```bat
gradlew.bat clean build
```

On Linux or macOS:

```sh
./gradlew clean build
```

The release JAR is written to `build/libs/` and includes `projectexa` and `arcanum` in its filename. The project uses the Gradle Wrapper and a minimal GitHub Actions build workflow.

## Links

- [Arcanum repository](https://github.com/WJXhhh/Project-Expansion-Arcanum)
- [Report a bug](https://github.com/WJXhhh/Project-Expansion-Arcanum/issues/new/choose)
- [Original Project Expansion repository](https://github.com/DonovanDMC/ProjectExpansion)
- [ProjectE](https://www.curseforge.com/minecraft/mc-mods/projecte)

## License and attribution

The original MIT license and Donovan_DMC copyright notice are retained. See [LICENSE](LICENSE) and [NOTICE](NOTICE.md). ProjectE attribution is retained; Arcanum is not an official ProjectE addon or ProjectE-supported project.
