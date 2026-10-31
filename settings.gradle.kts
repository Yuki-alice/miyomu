pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "miyomu"

includeBuild("build-logic")

include(
    ":app:shared",
    ":app:android",
    ":app:desktop",
    ":core:model",
    ":core:database",
    ":core:network",
    ":core:source",
    ":core:reader",
    ":core:data",
    ":core:design",
    ":core:ui",
    ":feature:library",
    ":feature:discover",
    ":feature:reader",
    ":feature:video",
    ":feature:downloads",
    ":feature:sync",
    ":feature:settings",
    ":feature:extensions",
    ":source:host",
)

project(":app:shared").projectDir = file("app/shared")
project(":app:android").projectDir = file("app/android")
project(":app:desktop").projectDir = file("app/desktop")
project(":source:host").projectDir = file("source/host")
