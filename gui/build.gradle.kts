plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.jvm)
}

group = "dev.fishies.sailfish"
version = "1.0.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.resources)
    implementation(libs.material.icons)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gradle.tooling)
    implementation(libs.slf4j.simple)

    implementation(projects.core)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
        optIn.add("kotlinx.coroutines.ExperimentalCoroutinesApi")
        optIn.add("kotlinx.coroutines.FlowPreview")
        optIn.add("androidx.compose.foundation.ExperimentalFoundationApi")
        freeCompilerArgs.add("-Xexplicit-backing-fields")
    }
}
