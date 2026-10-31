plugins {
    `java-gradle-plugin`
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
}

group = "app.miyomu.buildlogic"

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("com.android.tools.build:gradle:9.3.1")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    implementation("org.jetbrains.compose:compose-gradle-plugin:1.12.2")
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "miyomu.kmp.library"
            implementationClass = "MiyomuKmpLibraryPlugin"
        }
        register("kmpCompose") {
            id = "miyomu.kmp.compose"
            implementationClass = "MiyomuKmpComposePlugin"
        }
        register("jvmLibrary") {
            id = "miyomu.jvm.library"
            implementationClass = "MiyomuJvmLibraryPlugin"
        }
        register("jvmCompose") {
            id = "miyomu.jvm.compose"
            implementationClass = "MiyomuJvmComposePlugin"
        }
    }
}
