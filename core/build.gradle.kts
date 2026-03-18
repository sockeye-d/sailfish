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
    api(compose.foundation)
    api(compose.runtime)
    implementation(libs.compose.resources)

    compileOnly("com.google.devtools.ksp:symbol-processing-api:${libs.versions.kotlin.get()}")
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
