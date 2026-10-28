pluginManagement {
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/google")
        // Central mirror that carries Kotlin/Native .klib artifacts missing from Aliyun.
        maven("https://maven-central.storage-download.googleapis.com/maven2")
        maven("https://maven.aliyun.com/repository/central")
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "s1-player-ios"
