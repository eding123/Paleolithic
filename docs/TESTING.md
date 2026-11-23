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

### Mammoth herd

Add `gamerule spawn_mobs false` first (flat worlds spawn slimes). Then:

    0.5:kill @e[type=!player];3:summon ravager ~22 ~ ~ {Tags:["paleolithic.spawn.mammoth_herd"]};30:damage @e[tag=paleolithic.mammoth,tag=!paleolithic.leader,limit=1,sort=nearest] 4 minecraft:player_attack by Tester

The bot prints `SPAWN ravager`, `ATTR ... scale=1.400 max_health=120.000`
(adults) or `scale=0.500 max_health=30.000` (calves), the trumpet
(`entity.ravager.roar vol=6.0`), sweep tells (`entity.ravager.attack`) and
its own `HEALTH`. Start the server with `-Dpaleolithic.debugCreatures=true` to
log each creature's action, ground speed and, for calves, distance to the
nearest adult once per second.

Seen on Fabric, NeoForge and Paper on 2026-11-22: herd spawns at the right
sizes; grazing at 2.5 to 3 blocks/s; calves 2 to 4.5 blocks from an adult;
after a hit, charge tells and charges at 6.0; sweep damage 0.8 s after its
sound; whole herd flees at about 6 once a mammoth is below 25%; no loot;
herd and stats survive a restart (NeoForge); a dead player is left alone after
respawning (Paper).
