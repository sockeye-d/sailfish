plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.jvm)
}

group = "dev.fishies.ranim2"
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.material.icons)
    implementation(libs.kotlin.reflect)

    implementation(projects.core)
}

kotlin {
    jvmToolchain(21)
}
