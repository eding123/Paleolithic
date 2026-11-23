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

Only `rules` exists so far. The others are created in milestone 2 (below).

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

## Milestone 2 — platform modules (next)

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

    ./gradlew :rules:test

The wrapper uses Gradle 9.4.0, which Fabric Loom and ModDevGradle require for
Minecraft 26.x.
