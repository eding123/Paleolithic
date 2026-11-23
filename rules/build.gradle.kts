plugins {
    `java-library`
}

java {
    // The rules module stays on Java 21 bytecode so it also runs in tooling
    // and CI images that predate Java 25. The game itself runs on Java 25,
    // which loads Java 21 bytecode without issue.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:${property("junit_version")}")
}

tasks.test {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
