# Arcanum Testing Guide

This document records tests that can be automated in the repository and tests
that require a real Minecraft client or dedicated server.

## Automated build

Run with Java 17:

```text
gradlew.bat clean build
```

The build must produce a JAR whose filename starts with
`projectexa-arcanum-` and must expose the `projectexa` runtime namespace.

## Collector fractional EMC regression

Use a minimal instance containing only Forge 1.20.1, ProjectE PE1.0.1, and the
Arcanum JAR.

1. Set `collectorMultiplier=0.1` in the Arcanum config. The basic collector's
   nominal output is then below 1 EMC per second.
2. Test once with `enableCollectorOptimizations=false` and once with it set to
   `true`.
3. Place a basic Collector in a bright, loaded chunk and provide a Klein Star
   or a Project Expansion Star in its charging slot.
4. Leave the collector running long enough for fractional output to reach one
   whole EMC.
5. Verify that the star gains EMC and that the Collector does not accumulate
   EMC indefinitely after the star is able to accept it.
6. Repeat with an empty star, a partially charged star, and a star near its
   capacity.

Expected result: positive sub-1 EMC/t generation accumulates and is eventually
transferred as whole EMC. No EMC is lost, and zero generation transfers
nothing.

## Save migration regression

1. Create and save a world with official Project Expansion 1.1.3.
2. Include collectors, condensers, stars, player EMC, configured values, and
   any optional integration state that is available.
3. Shut down normally and back up the world.
4. Remove the official JAR, install only the Arcanum JAR, and load the backup.
5. Check for missing mappings, missing registry entries, vanished blocks/items,
   lost BlockEntity data, lost EMC, and lost configuration.

Expected result: all existing Project Expansion content remains present after
the old `projectexpansion` IDs are remapped to equivalent `projectexa` IDs.

## Runtime matrix still requiring manual observation

- Forge 47.2.0 baseline, 47.4.10 recommended, and 47.4.22 latest.
- Client and full EULA-enabled Dedicated Server startup.
- ProjectE PE1.0.1 with Arcanum only.
- Curios, JEI, Jade, WTHIT, The One Probe, AE2, and Pipez one at a time.
- Client/server EMC synchronization, restart persistence, and chunk unload/reload.
- Collector optimization on/off and all star capacity cases.

## Observed local server bootstrap

The local Java 17 `runServer --nogui` task was executed with Forge 47.2.0,
47.4.10, and 47.4.22, ProjectE PE1.0.1, and the Arcanum source set. The
EULA-enabled dedicated-server test reached `Done`, loaded the new source
namespace, and stopped normally. The client smoke run also reached a
responsive `Minecraft Forge 1.20.1` window.
