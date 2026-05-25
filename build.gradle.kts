import versioning.BuildConfig
import versioning.VersionUtil

BuildConfig.init(project)

val baseVersion = "2.0.0"
group = "dev.yanianz"
version = VersionUtil.computeVersion(project, baseVersion)
description = "SourbyAntiCheat — Server-side simulation anti-cheat for PaperMC with BungeeCord/Velocity proxy support."

ext["timestamp"] = System.currentTimeMillis().toString()
ext["git_branch"] = VersionUtil.getGitBranch(project, true)
ext["git_commit"] = VersionUtil.getGitCommitHash(project, true)
ext["git_org"] = System.getenv("SAC_GIT_ORG") ?: VersionUtil.getGitUser(project)
ext["git_repo"] = System.getenv("SAC_GIT_REPO") ?: "SourbyAntiCheat"
ext["build_shade_pe"] = BuildConfig.shadePE.toString()
ext["build_relocate"] = BuildConfig.relocate.toString()
ext["build_release"] = BuildConfig.release.toString()

tasks.register("printVersion") {
    group = "versioning"
    doLast { println("VERSION=$version") }
}

subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.isFork = true
        options.isIncremental = true
    }
}
