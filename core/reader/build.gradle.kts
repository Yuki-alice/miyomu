plugins {
    id("miyomu.kmp.library")
}

kotlin {

    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.ExperimentalStdlibApi",
            "-opt-in=kotlin.contracts.ExperimentalContracts",
            "-Xannotation-default-target=first-only",
        )
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:model"))
            implementation(libs.kotlinx.coroutines.core)
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
