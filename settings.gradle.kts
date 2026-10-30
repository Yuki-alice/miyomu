pluginManagement {
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

include(":shared:model")
include(":shared:network")
include(":shared:source-api")
include(":shared:source-runtime")
include(":shared:source-repo")
include(":shared:source-manager")
include(":shared:source-novel")
include(":shared:download")
include(":shared:danmaku")
include(":shared:data")
include(":shared:local")
include(":shared:novel")
include(":shared:reader-comic")
include(":shared:reader-novel")
include(":shared:player")
include(":shared:ui")
include(":shared:i18n")
include(":shared:i18n-aniyomi")
include(":shared:domain")
include(":shared:presentation-core")
include(":shared:presentation")
include(":desktopApp")
include(":androidApp")
