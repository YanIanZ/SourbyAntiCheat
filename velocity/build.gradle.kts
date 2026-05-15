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
    maven("https://nexus.scarsz.me/content/repositories/releases") {
        content { includeGroup("github.scarsz") }
    }
}

dependencies {
    implementation(project(":common"))
    compileOnly(libs.velocity.api)
    annotationProcessor(libs.velocity.api)
}

tasks.shadowJar {
    archiveClassifier = ""
    archiveFileName = "SAC-Velocity.jar"
}
tasks.build { dependsOn(tasks.shadowJar) }
