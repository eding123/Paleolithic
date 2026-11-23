# Changelog

All notable changes to Paleolithic are listed here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/). The release workflow publishes the
section whose heading matches the tag as the release notes.

## [Unreleased]

### Added
- Rules engine: body heat, shelter test, fire fuel and embers, lean diet,
  knapping, animal detection and wind, game pressure, carcass yields.
- Tests for every reference cold scenario in the design spec.
- Continuous integration and a tag-driven release workflow.
- Fabric, NeoForge and Paper builds for Minecraft 26.3 from one shared core.
- Body heat in game: the experience bar shows warmth and the action bar shows
  a status line (state, warming or cooling arrows) on unmodified clients.
  Biome, night, weather, blizzards, altitude, wetness, shelter, insulating
  blocks, campfires, lava, fire, magma blocks and torches all count. Cold
  slows you down and, on Normal and Hard, hypothermia hurts. Second Wind after
  respawning. Body heat is saved with the world.
