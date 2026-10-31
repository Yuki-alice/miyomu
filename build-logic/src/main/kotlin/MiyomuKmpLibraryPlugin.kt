import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class MiyomuKmpLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")

            val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
            val android = (kotlin as ExtensionAware).extensions
                .getByType(KotlinMultiplatformAndroidLibraryTarget::class.java)
            android.namespace = miyomuNamespace()
            android.compileSdk = 37
            android.minSdk = 26

            kotlin.jvm("desktop")
            kotlin.iosArm64()
            kotlin.iosSimulatorArm64()
        }
    }
}
