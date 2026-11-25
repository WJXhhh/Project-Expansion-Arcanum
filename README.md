# Project Expansion: Arcanum

Independent maintenance fork of [Project Expansion](https://github.com/DonovanDMC/ProjectExpansion) by Donovan_DMC. Maintained by JDJades. Not officially supported by ProjectE.

## Environment
- Minecraft 1.21.1
- NeoForge 21.1.219 or compatible newer 21.1 build
- Java 21
- ProjectE PE1.1.0

The runtime namespace remains `projectexa`. Porting features does **not** certify cross-version world migration. Do not open an irreplaceable 1.20.1 world in this development build.

## Development
`gradlew.bat compileJava` compiles Java sources. Normal `runClient` and `build` tasks use the checked-in `src/generated/resources`; they do not require Node.js or Bash. After editing resource templates, manually run `gradlew.bat data` to regenerate and commit the outputs. That optional task requires Node.js and npm on PATH; Gradle installs the locked dependencies and invokes Node directly.

Optional integrations must remain optional. This migration covers the stone table, Final Star, JEI transfer, personal EMC warehouse, projectiles and living-entity repair. Optional Ars Nouveau 5.13.3, Create 6.0.10 and Goety 3.2.1 material-supply integrations are now implemented. See [the API audit](docs/INTEGRATIONS_1_21_1.md) for version requirements and release-package caveats; game runtime acceptance is still pending.

The official TaCZ NeoForge release supports gun smith table material transmutation, per-model EMC, and warehouse ammo for players and Touhou Little Maid. [TaCZ EMC integration](docs/TACZ_EMC.md)

The Arcane Transmutation Tablet includes smithing, stonecutting, and anvil panels unlocked by learning the corresponding workstation. Carrying a tablet also enables EMC purchases of learned items through JEI and survival pick block (Shift picks a stack or tops up the held stack). Server options `enableJeiEmcRetrieval` and `enableWorldEmcPick` control these inputs. JEI retrieval targets 19.57.0.451 and 19.70.0.469, with a minimum of 19.57.0.451. Both sides must use the updated mod protocol.

## Beyond Dimensions and Beyond Craftlines

Carry an Arcane Transmutation Tablet in your inventory or Curios and enable the **EMC** switch below the crafting output in Beyond Dimensions 0.8.10. Hover the JEI or EMI fill button to see the materials and total price. Filling uses items in the grid, inventory and network before spending personal EMC. The switch starts off.

With Beyond Craftlines 0.7.3 (`for-bd0.8`), enable **EMC** in the order preview to supply missing ingredients. Alt-click a learned intermediate in the tree to switch between crafting and exchange. Hover the EMC switch or order button for the quote. Submitting pays once, and a price increase requires a fresh confirmation. Cancelling returns unused reservations to the network as items.

Run `gradlew.bat testBeyondEmc` with Java 21 for the standalone allocation and settlement checks.

See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md) for original attribution.
