# Testing

## Unit tests

    ./gradlew build

Runs the `rules` tests (including every reference cold scenario from the
spec) and the `core` tests, and builds the Fabric, NeoForge and Paper jars.

## In-game checks with an unmodified client

`testbot` joins a server the way an unmodified Minecraft client does and
prints what that client would see. It needs no game window.

1. Start a dev server for one platform. Each runs headless and keeps its world
   in a git-ignored folder:

   | Platform | Command | Folder | Port used below |
   |---|---|---|---|
   | Fabric | `./gradlew :fabric:runServer` | `fabric/run` | 25601 |
   | NeoForge | `./gradlew :neoforge:runServer` | `neoforge/runs/server` | 25602 |
   | Paper | `./gradlew :paper:runServer` | `paper/run` | 25603 |

   Before the first start, put these in the folder: `eula.txt` with
   `eula=true`; `server.properties` with `online-mode=false`, the port, and a
   flat snowy world
   (`level-type=minecraft\:flat` and `generator-settings` with
   `"biome":"minecraft:snowy_plains"`); `ops.json` making the bot an operator
   (offline UUID of `Tester`: `f3d28cb0-7225-3cb1-baeb-2dadd2be89ae`).

2. Run the bot with a script of `seconds:command` steps:

       ./gradlew -q :testbot:run --args="localhost 25601 Tester '1:time set noon;1.5:weather clear;2:gamerule spawn_monsters false;30:setblock ~2 ~ ~ campfire;50:time set midnight;65:quit'"

   Output, one line per update:

       [ 24.1] XP fill=0.600 level=0
       [ 24.1] BAR ♨ Warm ▼▼
       [ 25.1] XP fill=0.594 level=0
       [ 25.1] BAR ✹ Chilly ▼▼

   `XP fill` is the warmth bar (body heat / 100), `BAR` the status line. The
   bot respawns by itself after dying. `1:stop` shuts the server down.

3. Add `-Dpaleolithic.debugHeat=true` to the server's JVM arguments to log
   every heat step with its full felt-temperature breakdown.

### Expected results in the flat snowy world (biome base -14 C)

| Situation | Felt temperature | Bar change per second |
|---|---|---|
| Open, noon, clear | -14 C | -0.006 (▼▼) |
| Campfire 2 blocks away | about +7 C | about -0.001 (▼) |
| Same at midnight | about +1 C | about -0.002 (▼) |
| Snow-block hut with a doorway (insulated) | -4 C | -0.0035 (▼) |
| Stone hut with a doorway (sheltered) | -8 C | -0.0045 (▼) |
| Open in thunder (blizzard) | -22 C | -0.008 (▼▼) |
| First 30 s after respawn (Second Wind) | any | 0, bar at 0.75 |

All of these were seen on Fabric, NeoForge and Paper on 2026-11-22 (Paper
ran the hut and blizzard rows; Fabric the campfire and night rows; NeoForge
and Paper the respawn row).
