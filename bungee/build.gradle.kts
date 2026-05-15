plugins {
    id("sac.base-conventions")
    id("sac.shadow-conventions")
}

version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.grim.ac/snapshots") {
        content { includeGroup("ac.grim.grimac"); includeGroup("com.github.retrooper") }
}
tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName = "SAC-Bungee.jar"
}
tasks.build { dependsOn(tasks.shadowJar) }
