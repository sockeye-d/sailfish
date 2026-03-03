import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.composeHotReload)
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
    implementation(libs.treesitter)
    implementation(projects.languages.kotlin)
}

kotlin {
    jvmToolchain(21)
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
