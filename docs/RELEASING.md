# Releasing Paleolithic

Releases are made by pushing a version tag. The `Release` workflow then
builds, tests, attaches the jars and a `SHA256SUMS.txt` file, and publishes a
GitHub release with notes taken from `CHANGELOG.md`.

## Version scheme

- `mod_version` in `gradle.properties` follows Semantic Versioning:
  `1.0.0`, `1.1.0`, `1.0.1`.
- A hyphen marks a pre-release (`1.0.0-beta.1`). The workflow publishes those
  as GitHub pre-releases.
- Release jars carry the platform and Minecraft version in their file names,
  for example `paleolithic-fabric-1.0.0+26.3.jar`.

## Checklist

1. On `main`, with CI green, set `mod_version` in `gradle.properties`.
2. In `CHANGELOG.md`, rename `## [Unreleased]` to `## [1.0.0] - YYYY-MM-DD`
   and add a fresh empty `## [Unreleased]` above it.
3. Commit, then tag and push:

       git tag v1.0.0
       git push origin main v1.0.0

4. Watch the `Release` workflow. It stops before publishing if the tag does
   not match `mod_version`, if the changelog has no matching section, if any
   test fails, or if there are no artifacts.
5. After release, set `mod_version` to the next `-dev` version.

## How artifacts are collected

The root task `collectReleaseArtifacts` copies release files into
`build/release`. Each platform module registers its own output, for example in
`fabric/build.gradle.kts`:

    rootProject.tasks.named<Copy>("collectReleaseArtifacts") {
        from(tasks.named("remapJar"))
    }

The resource pack zip registers the same way. Until the platform modules
exist, nothing is collected and the workflow refuses to publish. That is
deliberate: the first release should be something a player can install.

## Later: mod hosting sites

Once 1.0 is close, the release workflow gains steps that publish the same
files to Modrinth, CurseForge and Hangar (for the Paper build). Those need
API tokens stored as repository secrets; none are needed yet.
