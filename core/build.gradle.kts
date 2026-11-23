// Shared game code. Compiled once against the vanilla game (NeoForm gives the
// unobfuscated, decompiled vanilla classes with no loader), then compiled
// again from source into each platform jar.
plugins {
    `java-library`
    id("net.neoforged.moddev")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

neoForge {
    neoFormVersion = providers.gradleProperty("neoform_version").get()
}

dependencies {
    api(project(":rules"))
}

dependencies {
    testImplementation("junit:junit:${property("junit_version")}")
}

// Unit tests that touch vanilla classes need the game on their runtime classpath.
configurations.named("testRuntimeClasspath") {
    extendsFrom(configurations.getByName("compileClasspath"))
}
