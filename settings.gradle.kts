pluginManagement {
    includeBuild("build-logic")
    repositories {
        // GitHub runners cannot reliably reach the Aliyun mirrors, so use the
        // canonical repositories there and keep the faster mirrors locally.
        if (System.getenv("CI") == "true") {
            google()
            mavenCentral()
            gradlePluginPortal()
        } else {
            google()
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
            maven("https://maven-central.storage-download.googleapis.com/maven2")
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (System.getenv("CI") == "true") {
            google()
            mavenCentral()
        } else {
            google()
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
            maven("https://maven-central.storage-download.googleapis.com/maven2")
            mavenCentral()
        }
    }
}

rootProject.name = "miyomu"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":shared:model")
include(":shared:network")
include(":shared:source-api")
include(":shared:source-runtime")
include(":shared:source-repo")
include(":shared:source-manager")
include(":shared:source-novel")
include(":shared:download")
include(":shared:danmaku")
include(":shared:track")
include(":shared:contract")
include(":shared:data")
include(":shared:local")
include(":shared:novel")
include(":shared:reader-comic")
include(":shared:reader-novel")
include(":shared:player")
include(":shared:ui-core")
include(":shared:ui-library")
include(":shared:ui-entries")
include(":shared:ui-novel")
include(":shared:ui-reader")
include(":shared:ui-sources")
include(":shared:ui-player")
include(":shared:ui-settings")
include(":shared:ui")
include(":shared:application")
include(":shared:i18n")
include(":shared:i18n-aniyomi")
include(":shared:domain")
include(":shared:presentation-core")
include(":shared:presentation")
include(":app:desktop")
include(":app:android")
