plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.moko)
}

kotlin {
    android {
        namespace = "tachiyomi.i18n"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.moko.resources)
        }
    }
}

multiplatformResources {
    resourcesPackage.set("tachiyomi.i18n")
}
