plugins {
    id("net.fabricmc.fabric-loom")
}

apply(from = rootProject.file("gradle/shared-sources.gradle.kts"))

val minecraftVersion = providers.gradleProperty("minecraft_version").get()

dependencies {
    // Minecraft 26.x is unobfuscated, so there are no mappings to apply.
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:${property("fabric_loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
}

sourceSets.main {
    resources.srcDir(rootProject.file("core/src/main/resources"))
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString(), "minecraft_version" to minecraftVersion)
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

rootProject.tasks.named<Sync>("collectReleaseArtifacts") {
    from(tasks.named("jar"))
}

loom {
    runs {
        named("server") {
            // Never open the server's window; tests run headless.
            programArgs("--nogui")
        }
    }
}
