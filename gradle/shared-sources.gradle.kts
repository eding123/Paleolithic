// Applied by every platform module. Each platform jar is self-contained: the
// rules and core sources are compiled straight into it (decision D13), so
// there is no nested jar and no shading step, and dev runs see the same
// classes as the release jar.

val sourceSets = extensions.getByType<SourceSetContainer>()

sourceSets.named("main") {
    java.srcDir(rootProject.file("rules/src/main/java"))
    java.srcDir(rootProject.file("core/src/main/java"))
}

extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

extensions.configure<BasePluginExtension> {
    archivesName.set("paleolithic-${project.name}")
}

// Release jars carry the platform and game version: paleolithic-fabric-1.0.0+26.3.jar
version = "${rootProject.version}+${providers.gradleProperty("minecraft_version").get()}"
