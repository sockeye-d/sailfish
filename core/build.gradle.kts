import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.compose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
}

group = "dev.fishies.ranim2"
version = "1.0.0"

dependencies {
    implementation(compose.foundation)
    implementation(compose.runtime)
    implementation(libs.compose.resources)
    implementation(libs.material.icons)
    implementation(libs.kotlin.reflect)
    implementation(libs.treesitter)
    implementation(libs.kotlinx.serialization.json)
    implementation(projects.languages.kotlin)
    implementation(projects.languages.odin)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.contracts.ExperimentalContracts")
        freeCompilerArgs.add("-Xcontext-parameters")
        freeCompilerArgs.add("-Xcontext-sensitive-resolution")
    }
}

compose.resources {}

compose.desktop {
    application {
        mainClass = "dev.fishies.ranim2.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.AppImage)
            packageName = group.toString()
            packageVersion = version.toString()
        }
    }
}
