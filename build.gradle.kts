plugins {
    base
}

allprojects {
    group = "dev.paleolithic"
    version = providers.gradleProperty("mod_version").get()
}

// Gathers every file a release ships into build/release. Platform modules
// register their outputs here (docs/RELEASING.md); the release workflow
// uploads whatever ends up in this folder.
tasks.register<Sync>("collectReleaseArtifacts") {
    group = "release"
    description = "Collects platform jars and the resource pack into build/release."
    into(layout.buildDirectory.dir("release"))
}
