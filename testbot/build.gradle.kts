// A headless stand-in for an unmodified Minecraft client, used to check that
// vanilla clients see what the spec promises (experience bar, status line).
// It reuses the vanilla client's own network code, so it speaks the exact
// protocol of minecraft_version. Not shipped; see docs/TESTING.md.
plugins {
    application
    id("net.neoforged.moddev")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

neoForge {
    neoFormVersion = providers.gradleProperty("neoform_version").get()
}

application {
    mainClass.set("dev.paleolithic.testbot.VanillaBot")
}

// The vanilla game and its libraries, for running the bot outside Gradle's
// compile step.
configurations.named("runtimeClasspath") {
    extendsFrom(configurations.getByName("compileClasspath"))
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}
