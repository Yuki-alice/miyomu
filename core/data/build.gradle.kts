plugins {
    id("miyomu.kmp.library")
}

kotlin {

    sourceSets {
        commonMain.dependencies {
            api(project(":core:model"))
            implementation(project(":core:database"))
            implementation(project(":core:network"))
            implementation(project(":core:source"))
            implementation(libs.kotlinx.coroutines.core)
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.junit.jupiter)
                implementation(libs.kotlinx.coroutines.test)
                runtimeOnly("org.junit.platform:junit-platform-launcher:1.11.4")
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
