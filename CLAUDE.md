# Paleolithic: working brief for Claude Code

Read this first, then `docs/ARCHITECTURE.md`.

## The owner

The owner does not want to deal with technical details. Make technical
decisions yourself, record each one with a short reason in
`docs/ARCHITECTURE.md` (next number in the Decisions list), and report results
in plain language. Ask only about things that change the game or cost money.

## The design

The full design lives in `docs/private/`. That folder is git-ignored on
purpose: the owner keeps the design private.

- `docs/private/DESIGN.md`: the spec. It is locked; numbers may move up to
  25% in balance passes, anything else needs a change note at its end.
- `docs/private/ITEMS-AND-RECIPES.txt` and `.pdf`: every item, recipe,
  knapping pattern, vanilla item disposition, and the carrier map.
- `docs/private/BESTIARY.txt` and `.pdf`: creature stats, behavior, spawns,
  carcass yields.

Never commit, quote at length, or paste anything from `docs/private/` into a
public file, commit message, issue or pull request. Code and code comments may
implement the rules, and short references such as "spec section 5" are fine.

## Hard requirements (spec section 20)

1. An unmodified Minecraft client can join a Paleolithic server on every
   supported platform and play every feature. The server sends the resource
   pack automatically.
2. Platforms: Fabric (also Quilt), NeoForge, Paper (also Purpur, Pufferfish),
   all from one codebase. Not supported: Spigot, Folia, Forge, hybrids.
3. The client mod is optional and only adds polish.
4. The carrier rule: no new registry entries. Items are vanilla stacks with
   data components; blocks are retextured carrier blocks (carrier map in the
   items file); creatures are vanilla base entities with replaced AI and
   display-entity models.
5. Release rule: 1.0 ships on all three platforms together, on the newest
   Minecraft version where Fabric, NeoForge and Paper all have stable builds.
   Java 25 for the platform modules.

## Current state

- `rules`: pure-Java game rules, done and tested (`./gradlew :rules:test`).
  Keep it free of Minecraft classes.
- CI and a tag-driven release workflow are in `.github/workflows`; see
  `docs/RELEASING.md`. Platform jars must register with
  `collectReleaseArtifacts`.
- Next: milestone 2 in `docs/ARCHITECTURE.md`. Pin toolchain versions, create
  `core`, then the `fabric`, `neoforge` and `paper` adapters with a vertical
  slice: body heat driving the experience bar and status line on an
  unmodified client, on all three platforms. Then the mammoth prototype.

## Working rules

- Small commits with clear messages; keep `CHANGELOG.md` "Unreleased" current.
- Run `./gradlew build` before every push; CI must stay green.
- Commit author is the owner's GitHub no-reply identity already set in this
  repo. End commit messages with:
  `Co-Authored-By: Claude <noreply@anthropic.com>`. Do not add links to
  private chats or sessions.
- Never commit secrets, local paths, or anything from `docs/private/`.
