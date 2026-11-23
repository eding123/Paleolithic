plugins {
    java
    id("io.papermc.paperweight.userdev")
    id("xyz.jpenilla.run-paper")
}

apply(from = rootProject.file("gradle/shared-sources.gradle.kts"))

val minecraftVersion = providers.gradleProperty("minecraft_version").get()

dependencies {
    // Paper API plus the server internals (vanilla classes) that core uses.
    paperweight.paperDevBundle(providers.gradleProperty("paper_dev_bundle").get())
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString(), "minecraft_version" to minecraftVersion)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }

    // Paper does not read data/ from a plugin jar's root. Core's data goes
    // into a pack folder that the bootstrapper registers at startup.
    into("paleolithic_pack") {
        from(rootProject.file("core/src/main/resources"))
        from(layout.projectDirectory.file("src/pack/pack.mcmeta"))
    }
}

tasks.runServer {
    minecraftVersion(minecraftVersion)
    jvmArgs("-Dcom.mojang.eula.agree=true")
    args("--nogui")
}

rootProject.tasks.named<Sync>("collectReleaseArtifacts") {
    from(tasks.named("jar"))
}
