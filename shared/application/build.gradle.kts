plugins {
    id("miyomu.kmp-compose")
}

kotlin {
    android {
        namespace = "app.miyomu.application"
    }

    sourceSets {
        commonMain.dependencies {
            // Public environment types: the platform entry points construct these.
            api(projects.shared.data)
            api(projects.shared.local)

            implementation(projects.shared.ui)
            implementation(projects.shared.domain)
            implementation(projects.shared.download)
            implementation(projects.shared.network)
            implementation(projects.shared.novel)
            implementation(projects.shared.sourceManager)
            implementation(projects.shared.sourceRepo)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.io.core)
            implementation(libs.ktor.client.core)
        }
    }
}
