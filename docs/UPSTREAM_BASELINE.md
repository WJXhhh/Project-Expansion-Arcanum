# Upstream Baseline

Recorded on 2026-08-16 before source or metadata changes.

## Repository

- Upstream repository: `https://github.com/DonovanDMC/ProjectExpansion.git`
- Upstream branch: `1.20.1`
- Fork repository: `https://github.com/WJXhhh/Project-Expansion-Arcanum.git`
- Working branch: `1.20.1-arcanum`
- Upstream commit: `4436aa664da9e26b63a47af9c3678631cace44b6`
- Upstream version: `1.1.3`

The Fork's `1.20.1` branch was identical to the upstream `1.20.1` branch at
the time of checkout. The local checkout has `origin` pointing at the Fork
and `upstream` pointing at the original repository.

## Toolchain

- Minecraft: `1.20.1`
- Java used for the baseline build: Eclipse Temurin `17.0.20`
- Gradle distribution used: `8.1.1`
- ForgeGradle resolved: `6.0.54`
- Forge: `47.2.0`
- ProjectE artifact: Curse Maven `4901949` (`PE1.0.1`)

The upstream checkout did not contain `gradlew` or `gradlew.bat`; Gradle
8.1.1 was therefore run from a temporary local distribution for this
baseline. Adding the Gradle Wrapper is part of the Arcanum maintenance work.

## Build

Command executed from a clean checkout:

```text
gradle --no-daemon --console=plain clean build
```

Result: **SUCCESS**

Baseline artifact:

- `build/libs/projectexpansion-1.20.1-1.1.3.jar`
- Size: `896075` bytes
- SHA-256: `F37A0EE1107A9999AE80AEC9B9DABD5CAE0EADD1AD54B2DF794058C79BD552C5`

The build emitted an encoding warning for Unicode symbols in
`ItemTooltipEvent.java` because the host's default compiler encoding was
GBK. The Arcanum build should make source encoding explicit without changing
runtime identifiers or game behavior.

## Compatibility Baseline

No runtime client, dedicated-server, or old-world migration test was
performed at this stage. Those remain release-gate tests after the baseline
and maintenance changes are applied.
