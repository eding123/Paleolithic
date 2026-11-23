# Paleolithic

[![CI](https://github.com/eding123/Paleolithic/actions/workflows/ci.yml/badge.svg)](https://github.com/eding123/Paleolithic/actions/workflows/ci.yml)

An Ice Age survival mod for Minecraft Java Edition: everything invented after
the Old Stone Age is gone, the world is frozen, and mammoths roam the steppe.

- Runs on **Fabric** (and Quilt), **NeoForge** and **Paper** (and Purpur,
  Pufferfish).
- Players with an unmodified client can join any supported server; the server
  sends a resource pack automatically. Installing the mod on the client is
  optional and adds a smoother HUD and dedicated screens.

The full design is in the "Paleolithic — Minecraft Mod Design Document" spec.
Technical decisions and the module layout are in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Status

| Part | State |
|---|---|
| `rules` — heat, shelter, fire, diet, knapping, detection, wind, game pressure, carcasses | Implemented, 67 tests passing |
| `core`, `fabric`, `neoforge`, `paper`, `client` | Next milestone |

## Build

Requires a JDK 21 or newer (Minecraft 26.x itself needs Java 25).

    ./gradlew :rules:test

## Releases

There are no releases yet: the first one will be a playable build. Releases
are published automatically from version tags; see
[docs/RELEASING.md](docs/RELEASING.md) and [CHANGELOG.md](CHANGELOG.md).

## License

[MIT](LICENSE)
