plugins {
    id("sac.base-conventions")
    id("sac.shadow-conventions")
}

ext["version"] = rootProject.version.toString()

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.grim.ac/snapshots") {
        content { includeGroup("ac.grim.grimac"); includeGroup("com.github.retrooper") }
    }
    maven("https://nexus.scarsz.me/content/repositories/releases") {
        content { includeGroup("github.scarsz") }
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.waterfall.api)
    implementation(libs.adventure.platform.bungeecord)
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName = "SAC-Bungee.jar"
}
tasks.build { dependsOn(tasks.shadowJar) }
