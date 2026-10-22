# Project Expansion: Arcanum

Independent maintenance fork of [Project Expansion](https://github.com/DonovanDMC/ProjectExpansion) by Donovan_DMC. Maintained by JDJades. Not officially supported by ProjectE.

## Environment
- Minecraft 1.21.1
- NeoForge 21.1.219 or compatible newer 21.1 build
- Java 21
- ProjectE PE1.1.0

The runtime namespace remains `projectexa`. Porting features does **not** certify cross-version world migration. Do not open an irreplaceable 1.20.1 world in this development build.

## Development
`gradlew.bat compileJava` compiles Java sources. `gradlew.bat build` also invokes the asset generator, which requires the script runtime.

Optional integrations must remain optional. This migration covers the stone table, Final Star, JEI transfer, personal EMC warehouse, projectiles and living-entity repair. Optional Ars Nouveau 5.13.3, Create 6.0.10 and Goety 3.2.1 material-supply integrations are now implemented. See [the API audit](docs/INTEGRATIONS_1_21_1.md) for version requirements and release-package caveats; game runtime acceptance is still pending.

See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md) for original attribution.
