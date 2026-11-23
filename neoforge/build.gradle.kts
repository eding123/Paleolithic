plugins {
    id("net.neoforged.moddev")
}

apply(from = rootProject.file("gradle/shared-sources.gradle.kts"))

val minecraftVersion = providers.gradleProperty("minecraft_version").get()

neoForge {
    version = providers.gradleProperty("neoforge_version").get()

    runs {
        register("server") {
            server()
            programArgument("--nogui")
            gameDirectory = file("runs/server")
        }
    }

    mods {
        register("paleolithic") {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.main {
    resources.srcDir(rootProject.file("core/src/main/resources"))
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to minecraftVersion,
        "neoforge_version" to providers.gradleProperty("neoforge_version").get(),
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") { expand(props) }
}

rootProject.tasks.named<Sync>("collectReleaseArtifacts") {
    from(tasks.named("jar"))
}
