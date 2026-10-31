plugins {
    id("miyomu.kmp.library")
    id("miyomu.kmp.compose")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:ui"))
            implementation(project(":core:data"))
            implementation(project(":core:design"))
            implementation(project(":core:model"))
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.koin.viewmodel)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit.jupiter)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
            }
        }
    }
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
