plugins {
    base
}

allprojects {
    group = "dev.paleolithic"
    version = providers.gradleProperty("mod_version").get()
}
