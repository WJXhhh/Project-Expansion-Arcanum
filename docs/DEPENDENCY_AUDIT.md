# Dependency Audit

Audited on 2026-08-16 against the upstream `1.20.1 / 1.1.3` baseline.

No dependency version was changed for the first Arcanum maintenance pass.
The intent is to keep the upstream runtime environment stable while the
Forge and optional-integration matrix is tested one change at a time.

| Dependency | Current baseline | Upgrade decision | Reason |
| --- | --- | --- | --- |
| Minecraft | 1.20.1 | Keep | Project scope is 1.20.1. |
| Forge | 47.2.0 | Keep for baseline | The upstream build and first local verification use 47.2.0. 47.4.10 and 47.4.22 remain release-gate test targets. |
| ProjectE | Curse Maven `4901949` / PE1.0.1 | Keep | Primary addon compatibility target. |
| Curios | `5.4.2+1.20.1` | Keep | Optional integration; no dependency sweep. |
| JEI | `15.2.0.27` | Keep | Optional integration; no dependency sweep. |
| Jade | API `5073670`, runtime `5072729` | Keep | Optional integration; no dependency sweep. |
| WTHIT | `5208290` | Keep | Compile-only optional integration; no dependency sweep. |
| The One Probe | `1.20.1-10.0.1-3` | Keep | Compile-only optional integration; no dependency sweep. |
| AE2 | `4857895` | Keep | Optional runtime integration; no dependency sweep. |
| Pipez | `4818852` | Keep | Optional runtime integration; no dependency sweep. |
| No Chat Reports | `4610474` | Keep | Runtime test dependency inherited from upstream. |
| iTank | `4647928` | Keep | Runtime optional integration inherited from upstream. |
| Cloth Config | `4633444` | Keep | Runtime optional integration inherited from upstream. |
| Better Advancements | `4985146` | Keep | Runtime optional integration inherited from upstream. |
| Brigadier | `1.0.18` | Keep | Direct implementation dependency inherited from upstream. |
| ForgeGradle | resolved `6.0.54` | Keep | Required by the upstream build; no toolchain sweep. |
| Mixin | `0.8.5` processor / mixingradle `0.7-SNAPSHOT` | Keep | Existing Mixin setup and refmap names are compatibility-sensitive. |

Optional integrations remain compile-only or runtime-only as defined by the
upstream build. Missing-optional-mod startup checks are still required before
release.
