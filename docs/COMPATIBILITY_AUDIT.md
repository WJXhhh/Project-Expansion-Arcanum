# Compatibility Audit

Audited on 2026-08-16. The source diff is limited to the Collector precision
fix; the remaining changes are project metadata, documentation, and build
infrastructure.

## Runtime identity

The following identifiers were checked and intentionally preserved:

- `modId = projectexpansion`
- `projectexpansion` registry/resource namespace
- Java package `cool.furry.mc.forge.projectexpansion`
- existing registry declarations and generated resource paths
- configuration file keys in `Config.java`
- NBT key constants in `TagNames` and existing BlockEntity serialization
- packet channel `projectexpansion:primary`
- packet protocol version `1` and registration order
- `projectexpansion.mixins.json` and `projectexpansion.refmap.json`

No registry IDs, NBT keys, SavedData keys, config keys, language keys, recipe
IDs, or network identifiers were renamed.

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
dev run does not resolve the production refmap location; it reached the EULA
gate without a Mixin failure. A client and an EULA-enabled dedicated-server
run are still required for release acceptance.

## Compatibility status

No breaking data-format change was made. The official 1.1.3 world migration,
reverse migration, Forge 47.4.10/47.4.22 matrix, client smoke tests, and
optional-integration matrix remain manual release-gate work.
