# Paleolithic — architecture and technical decisions

The design spec (the "Paleolithic — Minecraft Mod Design Document" doc) is the
source of truth for gameplay. This file records the technical decisions made
while implementing it. Each decision has a short reason so it can be revisited
deliberately rather than by accident.

## Module layout

| Module     | Depends on            | Contents |
|------------|-----------------------|----------|
| `rules`    | nothing               | Pure game rules: body heat, shelter test, fire fuel, lean diet, knapping, detection, wind, game pressure, carcass yields. No Minecraft classes. |
| `core`     | `rules`, vanilla game | Carrier layer, model runtime, crafting matcher, menus, HUD, data loading, world generation data pack. Compiled once against unobfuscated vanilla code. |
| `fabric`   | `core`, Fabric API    | Fabric adapter (also used on Quilt). |
| `neoforge` | `core`                | NeoForge adapter. |
| `paper`    | `core`, Paper API     | Paper adapter (also Purpur, Pufferfish). |
| `client`   | `core`                | Optional client enhancements for Fabric and NeoForge. |
| `testbot`  | vanilla game          | Headless stand-in for an unmodified client, for testing only (docs/TESTING.md). |

`rules`, `core`, `fabric`, `neoforge`, `paper` and `testbot` exist. `client`
comes later.

## Decisions

**D1. A pure-Java `rules` module.** Everything that is arithmetic or a state
machine lives here, with no Minecraft types. Reason: it is the most
balance-sensitive code, it must behave identically on three platforms, and it
can be tested in milliseconds without a game. The spec's 13 heat reference
scenarios (acceptance criterion 2) run as plain unit tests.

**D2. The platform layer samples, the rules decide.** Rules receive plain
inputs (`Surroundings`, `BlockProbe`) and return plain results (`StepResult`,
`ShelterTest.Result`). The platform layer applies effects, damage and HUD.
Reason: no world access in rules means no threading or platform surprises.

**D3. Java 21 bytecode for `rules`, Java 25 for everything else.** Minecraft
26.x requires Java 25, but `rules` has no reason to need it, and Java 21
bytecode keeps it usable in older tooling and CI images.

**D4. JUnit 4 for `rules`.** Small, stable and enough for pure unit tests.
In-game behavior is covered by game tests in milestone 2.

**D5. Underground ignores the shelter bonus.** The spec says caves are a flat
+6 C with surface modifiers ignored, and reference scenario 12 ("any cave,
hide set: 20 C") only holds if the shelter bonus is also skipped underground.
The scenario table is the contract, so `FeltTemperature` skips it.

**D6. A cold fire refuses fuel.** The spec says lighting gives 4:00 of fuel but
does not say what fuel added to an unlit fire does. Refusing it (with the
"light it first" hint) avoids hidden stored fuel.

**D7. Fires tick only while loaded, and large steps carry over between
states.** Same as vanilla block entities. A long step (for example after lag)
correctly moves a fire from lit through embers to cold.

**D8. Lean-diet penalties use the count before the meal.** A fatty meal eaten
while on a Lean Diet is itself penalised but lifts the status afterwards,
which keeps the rule simple to explain.

**D9. Carcass cuts are an even, order-preserving split.** Bestiary yields give
totals and a cut count; `CarcassYield.plan` spreads the items across cuts in
listed order, so hides and meat always come first. Legends get twice the
cuts as well as twice the yield.

**D10. Pattern changes are allowed only while no needed cell is chipped.** The
spec says a piece can never be ruined; this extends that guarantee to
switching patterns mid-way.

**D11. Item IDs for knapped heads are `paleolithic:<material>_<pattern>`**,
for example `paleolithic:fine_spear_point`. Under the carrier rule these are
the ids stored in an item's custom data, not registry entries.

**D12. Group id `dev.paleolithic`.** A placeholder namespace; change it before
the first public release if you publish under your own domain or GitHub name.

**D13. Each platform jar compiles `rules` and `core` from source.** The
platform modules add both source folders to their own `main` source set
(`gradle/shared-sources.gradle.kts`). One self-contained jar per platform, no
nested jars or shading, and dev runs use exactly the release classes. `core`
still compiles on its own, so code that only works on one platform fails there.

**D14. `core` compiles against plain vanilla via NeoForm.** ModDevGradle's
`neoFormVersion` gives the unobfuscated vanilla game with no loader on the
classpath, so `core` cannot accidentally use Fabric, NeoForge or Paper types.

**D15. Per-player data lives in `<world>/paleolithic/players/<uuid>.dat`.**
Written by core with vanilla NBT, saved on leave and server stop, written
through a temporary file. Same behaviour on every platform, no platform
attachment APIs, and it moves with the world folder.

**D16. The vanilla HUD is a sent packet, not the player's real experience.**
Every second core sends the experience bar with fill = B/100 and level 0
(which hides the number), plus the status line in the action bar. The
player's real experience is untouched; until experience is removed (spec
section 17 item table), a vanilla experience change can show its own value
for under a second before the next send overwrites it.

**D17. Heat sources are found by a cube scan of radius 9 around the eyes.**
About 6,900 block reads per player per second. Only vanilla sources so far:
lit campfire, lava, fire, magma block, torch. The strongest single source
counts. Hearth, embers, stone lamp and vents arrive with their carriers; a
cached source index replaces the scan if profiling asks for it.

**D18. Paper gets core's data through a bootstrapper.** Paper does not read
`data/` from a plugin jar, so the Paper jar carries it in `paleolithic_pack/`
and `PaleolithicBootstrap` registers it at data-pack discovery. Paper loads it
automatically ("Found new data pack Paleolithic/game_data").

**D19. Gradle runs on Java 25.** `gradle/gradle-daemon-jvm.properties` pins the
build JVM to Temurin 25 (downloaded automatically when missing), because Loom
for 26.x needs it. `rules` still targets Java 21 bytecode (D3).

**D20. Vanilla-client checks use `testbot`.** No public bot library speaks
26.3 yet, so `testbot` reuses the vanilla client's own network code: it logs
in offline, answers keep-alives, prints every experience-bar and status-line
update, and runs a timed script of commands. This checks the hardest
requirement (unmodified clients) on all three platforms without opening a
game window.

**D21. Creative and spectator players do not cool.** Their HUD keeps showing
the last value. Matches how vanilla treats hunger.

**D22. Stand-in AI is replaced through the goal selectors, by reflection.**
`Mob.goalSelector` and `targetSelector` are protected; core reaches them with
reflection (`creature/MobAccess`). Field names are the same on every
platform because 26.x is unobfuscated, and it was checked on Fabric, NeoForge
and Paper. One goal per creature (`MammothBrain`) owns movement, looking and
jumping and runs the whole behaviour as a small state machine.

**D23. Creatures are marked with scoreboard tags.** `paleolithic.mammoth`,
`paleolithic.mammoth_calf`, `paleolithic.herd.<id>`, `paleolithic.home.<x>_<y>_<z>`
and `paleolithic.leader` are saved with the entity by vanilla. When an entity
loads or spawns, each adapter forwards it to core, which re-applies the AI at
the end of the tick. Short-lived herd mood (anger, flight) is in memory only.

**D24. Speeds are converted with the square law.** A walking mob's top speed
is 44.05 x s^2 blocks per second for s = attribute x modifier (the move
control uses s as both speed and input). Measured in game: grazing 2.5 to 3,
charge 6.0, flight about 6.

**D25. Until natural spawning exists, herds are placed with a vanilla
command.** `/summon ravager ~ ~ ~ {Tags:["paleolithic.spawn.mammoth_herd"]}`
becomes a herd of 3 to 6 with 1 to 2 calves. No platform command code needed.

**D26. Ravager loot is empty.** Mammoth stand-ins would otherwise drop
saddles, which the spec bans; ravagers do not exist otherwise in an Ice Age
world. Carcasses replace this when they arrive.

**D27. A herd forgets a player who dies.** Without this, a herd kept
attacking the respawned player for the rest of its 30 seconds of anger (spec
rule 4, fail forward).

## Milestone 2 — platform modules

Status 2026-11-22: steps 1 to 3 done. Toolchain pinned to Minecraft 26.3
(stable on Fabric, NeoForge and Paper) in `gradle.properties`. The body heat
vertical slice runs on all three platforms and was checked with `testbot`
(docs/TESTING.md): bar and status line on an unmodified client, cooling rates,
campfire, night, shelter, insulation, blizzard, death and Second Wind, saving
across rejoin and restart. Not done in the slice: health-regeneration and
hunger penalties of the cold states, wetness from rain was not exercised,
clothing, hints in chat, the resource pack.

Step 4, mammoth prototype (2026-11-22): playable on all three platforms on a
vanilla client. Herd of 3 to 6 with calves, stand-in at the spec's scale and
health, grazing around a home point, calves within 6 blocks of an adult,
trumpet every 1 to 3 minutes, defence when hurt or when a player is near a
calf, tusk sweep (14 after a 0.8 s tell), charge (10 blocks at 6.0 after a
1.0 s tell), herd flight below 25% health, saved across restarts. Not in the
prototype: detection modifiers beyond night and sneaking (wind, blizzard),
avoiding deep water and crevasses, carcasses, natural spawning, herd
migration and calf births, the display-entity model for players with the
pack, custom navigation for big bodies, and Peaceful difficulty (vanilla
removes ravagers there).


Needs a machine with network access, because the build has to download
Minecraft, Fabric Loom, NeoForge ModDevGradle and paperweight-userdev.

1. Pin toolchain versions for the target game version: Loom, Fabric API,
   NeoForge, ModDevGradle, Paper API and paperweight. Apply the release rule:
   26.3 only if all three platforms have stable builds, otherwise 26.2.
2. Create `core` with the platform-hook interfaces and the carrier layer for
   items (data components) first, since every later system needs items.
3. Create the three adapters with a "hello world" vertical slice: the body
   heat loop driving the experience bar and status line on a vanilla client.
   This proves the hardest requirement (vanilla clients on every platform)
   before content work starts.
4. First production milestone from the spec's risk table: a playable mammoth
   prototype on a vanilla client, on all three platforms.

## Running the tests

    ./gradlew build

runs every unit test and builds the three platform jars. In-game checks with
the test client are in docs/TESTING.md.

The wrapper uses Gradle 9.9.2 (kept current by Dependabot); Fabric Loom and
ModDevGradle need Gradle 9 or newer for Minecraft 26.x.
