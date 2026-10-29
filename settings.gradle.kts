pluginManagement {
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        mavenCentral()
    }
}

rootProject.name = "miyomu"

include(":shared:model")
include(":shared:network")
include(":shared:source-api")
include(":shared:source-runtime")
include(":shared:data")
include(":shared:local")
include(":shared:reader-comic")
include(":shared:player")
include(":shared:ui")
include(":shared:i18n")
include(":shared:i18n-aniyomi")
include(":shared:domain")
include(":shared:presentation-core")
include(":shared:presentation")
include(":desktopApp")
include(":androidApp")
