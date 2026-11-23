pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
        maven("https://repo.papermc.io/repository/maven-public/") { name = "PaperMC" }
    }
    plugins {
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version").get()
        id("net.neoforged.moddev") version providers.gradleProperty("moddev_version").get()
        id("io.papermc.paperweight.userdev") version providers.gradleProperty("paperweight_version").get()
        id("xyz.jpenilla.run-paper") version providers.gradleProperty("run_paper_version").get()
    }
}

plugins {
    // Downloads the Java 25 toolchain when the machine does not have one.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "paleolithic"

// Pure-Java game rules. No Minecraft dependency; builds and tests anywhere.
include("rules")
// Shared game code compiled against vanilla Minecraft (see docs/ARCHITECTURE.md).
include("core")
// Platform adapters. Each ships rules + core + its own entry points in one jar.
include("fabric")
include("neoforge")
include("paper")
include("testbot")
