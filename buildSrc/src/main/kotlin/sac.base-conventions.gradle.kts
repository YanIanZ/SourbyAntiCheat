plugins {
    id("java-library")
    id("io.freefair.lombok")
    id("com.diffplug.spotless")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
}

spotless {
    java {
        target("src/**/*.java")
        endWithNewline()
        removeUnusedImports()
        indentWithSpaces(4)
        trimTrailingWhitespace()
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.gradle.kts")
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.processResources {
    filesMatching("**/*.properties") { expand(project.properties) }
    filesMatching("**/plugin.yml") { expand(project.properties) }
    filesMatching("**/bungee.yml") { expand(project.properties) }
}
