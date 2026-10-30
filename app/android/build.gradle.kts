plugins {
    id("miyomu.android-application")
}

android {
    namespace = "app.miyomu.android"

    defaultConfig {
        applicationId = "app.miyomu.android"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.shared.application)
    implementation(projects.shared.data)
    implementation(projects.shared.local)
    implementation(libs.androidx.activity.compose)
}
