plugins {
    id("miyomu.kmp-compose")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "app.miyomu.ui.core"
    }

    sourceSets {
        commonMain.dependencies {
            // MiyomuGraph's public signature spans every type below, so they are all api:
            // consumers compile against the graph, not against accidental transitive exposure.
            api(projects.shared.model)
            api(projects.shared.contract)
            api(projects.shared.domain)
            api(projects.shared.local)
            api(projects.shared.novel)
            api(projects.shared.readerNovel)
            api(projects.shared.sourceRepo)
            api(projects.shared.sourceManager)
            api(projects.shared.download)
            api(projects.shared.danmaku)
            api(projects.shared.sourceNovel)
            implementation(projects.shared.track)
            // Required by MiyomuGraph's public `readerPreferences` (ReaderPreferences is a
            // Compose-touching preference group that still lives in presentation); this is a
            // signature-driven api, not a blanket re-export.
            api(projects.shared.presentation)
            api(libs.navigation3.runtime)
            api(libs.coil.compose)
            api(libs.coil.network.okhttp)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(projects.shared.data)
        }
    }
}
