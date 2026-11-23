rootProject.name = "paleolithic"

// Pure-Java game rules. No Minecraft dependency; builds and tests anywhere.
include("rules")

// Platform modules are added in the next milestone, once the build runs on a
// machine with network access (they need Minecraft, Fabric Loom, NeoForge
// ModDevGradle and paperweight-userdev):
//
//   core      - carrier layer, model runtime, crafting matcher, menus, HUD, data
//   fabric    - Fabric adapter (+ Quilt)
//   neoforge  - NeoForge adapter
//   paper     - Paper adapter (+ Purpur, Pufferfish)
//   client    - optional client enhancements (Fabric and NeoForge)
//
// See docs/ARCHITECTURE.md.
