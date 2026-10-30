plugins {
    id("miyomu.jvm-compose")
}

dependencies {
    implementation(projects.shared.application)
    implementation(projects.shared.data)
    implementation(projects.shared.local)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "app.miyomu.desktop.MainKt"
    }
}
