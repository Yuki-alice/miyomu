import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

val libs = versionCatalogs.named("libs")

configure<KotlinMultiplatformExtension> {
    jvmToolchain(MiyomuBuild.toolchain)

    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    android {
        compileSdk = MiyomuBuild.compileSdk
        minSdk = MiyomuBuild.minSdk
        androidResources.enable = true
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(MiyomuBuild.jvmTarget.toString()))
        }
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        // JVM-only implementations shared by the desktop and Android targets.
        jvmMain { kotlin.srcDir("src/jvmShared/kotlin") }
        androidMain { kotlin.srcDir("src/jvmShared/kotlin") }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.findLibrary("junit-jupiter").get())
            runtimeOnly("org.junit.platform:junit-platform-launcher")
        }
    }
}
