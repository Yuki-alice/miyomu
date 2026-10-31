plugins {
    id("miyomu.jvm.library")
}

dependencies {
    implementation(project(":core:source"))
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
}

