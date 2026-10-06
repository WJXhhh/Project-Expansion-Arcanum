# Changelog

Entries for 1.1.7 through 1.1.11 were reconstructed from git history; they were not
recorded while the work was done. `mod_version` went 1.1.4-arcanum.1 to 1.1.7 to 1.1.9
to 1.1.10 to 1.1.11 in the repository, so 1.1.5, 1.1.6 and 1.1.8 have no recorded content.

## Unreleased

### Added

- Bound Personal EMC Warehouses equipped in a Touhou Little Maid's Curios slots now supply
  ammunition for TACZ guns, in addition to warehouses carried in her inventory.
- ProjectE now calculates EMC for TACZ guns, ammunition and attachments automatically.
  Gun smith table recipes are mapped through ProjectE's own `@RecipeTypeMapper` extension
  point, so the values are derived from the recipe materials by ProjectE's graph solver
  during the normal EMC mapping pass (datapack load and `/reload`), not from a fixed table.
- Added a ProjectE `@NBTProcessor` that reduces a TACZ gun, cartridge or attachment to the
  identity NBT its value is registered under, so a gun keeps its EMC after loading ammo,
  installing attachments, gaining levels or changing skins. Guns created by transmutation are
  byte for byte identical to gun smith table output, including the default fire mode.
- Added `docs/TACZ_EMC.md` describing the design, the ProjectE-side toggles
  (`CraftingMapper.TACZGunSmithTable`, `processors.TACZItemProcessor`) and the known tradeoffs.

### Changed

- Re-enabled the Personal EMC Warehouse, including its crafting recipe and advancement,
  with the completed local warehouse and TACZ integrations preserved.
- EMC lookups now consult an item's persistent NBT form before falling back to the bare item,
  which is what lets mods that keep their item identity in NBT carry per-variant values.
  Controlled by the new `persistentNbtEmcFallback` config option (default on); items that
  already resolve today are unaffected.

### Fixed

- Fixed a crash when syncing or clicking Touhou Little Maid inventory slots while a bound
  Personal EMC Warehouse is present. Direct writes now delegate to the physical inventory;
  virtual warehouse slots continue to require EMC-accounted insertion and extraction.

## 1.1.11 — 2026-09-01

- Version bump only.

## 1.1.10 — 2026-09-01

### Added

- Added the Personal EMC Warehouse: a Curios-compatible item with its own screen, container and
  inventory handler that stores items as EMC, plus its recipe, advancement and model.
- Made a bound warehouse in a Touhou Little Maid inventory visible to TACZ's reload scan, so a
  maid can feed guns from it.
- Let the warehouse serve TACZ ammunition by matching the concrete ammunition id stored in NBT.

## 1.1.9 — 2026-08-31

### Added

- Gave the Final Star a purpose: on an active pedestal it copies items dropped above it into an
  adjacent container, with three new options: `finalStarCopyAnyItem`, `finalStarCopyNBT` and
  `finalStarUpdateInterval`.

### Fixed

- Stopped the Arcane Transmutation Tablet from re-learning an item taken out of the unlearn slot
  and consuming the physical stack into EMC when the screen was closed.
- Made the TACZ gun smith table screen Mixin match both the development and reobfuscated method
  names, so the transmute button also appears outside the development environment.

## 1.1.7 — 2026-08-27

### Added

- Added JEI recipe transfer for the Arcane Transmutation Tablet, including transfer from the
  player crafting grid and a universal handler, with a JEI-style button widget.
- Added the TACZ integration: a button on the gun smith table screen that transmutes the
  materials of the selected recipe, with server-side revalidation of the recipe and its inputs.
- Added the `/projectexa reloademc` command and its permission entry.
- Added a key binding and packet to open the Arcane Transmutation Tablet, and registered the
  tablet as a Curios item.

## 1.1.4-arcanum.1 — 2026-08-17

### Maintenance

- Established Project Expansion: Arcanum as an independently maintained Minecraft 1.20.1 fork.
- Added the Gradle Wrapper and a minimal GitHub Actions build workflow.
- Made Java source encoding explicit for reproducible builds.
- Added baseline, attribution, migration, and issue-reporting documentation.

### Compatibility

- Changed the runtime Mod ID and registry/resource namespace to `projectexa` to avoid namespace collisions.
- Added Forge missing-mapping remaps for official `projectexpansion` blocks, items, menus, enchantments, and sounds, plus a block-entity NBT migration hook.
- Kept the upstream Forge `47.2.0` and ProjectE `PE1.0.1` baseline while maintenance verification is in progress.

### Branding

- Updated project metadata and documentation to Project Expansion: Arcanum.
- Redirected update checking to the Arcanum repository's update JSON.

### Fixed

- Preserved fractional Collector output so low-rate generation can accumulate instead of being truncated to zero.
- Allowed positive sub-1 EMC/t Collector output to transfer once a whole EMC has accumulated, including to Klein Stars.

### Added

- Added the Arcane Transmutation Tablet and the Stone Transmutation Table.
- Added a Tome of Knowledge recipe.
