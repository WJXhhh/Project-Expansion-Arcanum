# Compatibility Audit

Audited on 2026-08-16. The Arcanum runtime namespace migration is intentional
and is separate from the Collector precision fix.

## Runtime identity

The following implementation identifiers remain stable:

- Java package `cool.furry.mc.forge.projectexpansion`
- configuration file keys in `Config.java`
- NBT key constants in `TagNames` and existing BlockEntity serialization
- packet protocol version `1` and registration order

The runtime Mod ID, registry/resource namespace, language keys, datapack
namespace, packet channel, and Mixin/refmap filenames now use `projectexa`.
The old `projectexpansion` namespace is handled by Forge missing mappings for
registered Forge objects. A small Mixin also rewrites the legacy block-entity
ID stored inside chunk NBT before vanilla resolves it. The old Java package is
intentionally retained.

## Numeric boundary review

The Collector path was reviewed against the upstream fix history. The
reproducible problem was that fractional generation was converted to a
`BigInteger` before it could accumulate or transfer. The fix keeps the
generation rate as `BigDecimal`, retains the existing `unprocessedEMC`
remainder, and uses a whole-EMC transfer limit only at the ProjectE capability
boundary.

Other primitive conversions remain in the existing UI or Forge/ProjectE API
boundaries. They were not changed without a demonstrated compatibility or
overflow failure.

## Mixin verification

The production artifact contains both the Mixin configuration and refmap. A
development `runServer` bootstrap emitted the known refmap warning because the
dev run does not resolve the production refmap location; it reached `Done`
without a Mixin failure. The client smoke test and EULA-enabled dedicated-server
test both reached a responsive/ready state.

## Compatibility status

The namespace change is a breaking identifier change for new commands and
resource lookups, with an explicit official 1.1.3 world migration path. The
official 1.1.3 world migration and reverse migration have been exercised with
representative block entities and item NBT. Forge 47.4.10/47.4.22 matrix and
the optional-integration matrix remain release-gate work.
