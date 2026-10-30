plugins {
    id("miyomu.kmp-library")
    alias(libs.plugins.moko)
}

kotlin {
    android {
        namespace = "tachiyomi.i18n.aniyomi"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.moko.resources)
        }
    }
}

multiplatformResources {
    resourcesClassName.set("AYMR")
    resourcesPackage.set("tachiyomi.i18n.aniyomi")
}
