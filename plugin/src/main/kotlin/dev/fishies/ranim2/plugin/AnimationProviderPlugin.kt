package dev.fishies.ranim2.plugin

import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

const val composeVersion = "1.10.0"
const val kotlinVersion = "2.3.0"

class AnimationProviderPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.applyAnimationProviderPlugin()
    }
}

private fun Project.applyAnimationProviderPlugin() {
    plugins.apply("org.jetbrains.compose")
    plugins.apply("org.jetbrains.kotlin.plugin.compose")
    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("com.google.devtools.ksp")

    val jsonFile = projectDir.resolve("metadata.json")
    val jarFilePath = tasks.withType<Jar>().first().outputs.files.singleFile

    extensions.configure<KspExtension> {
        arg("jsonFile", jsonFile.absolutePath)
        arg("jarFile", jarFilePath.absolutePath)
    }

    dependencies {
        add("implementation", "org.jetbrains.compose.components:components-resources:$composeVersion")
        add("implementation", "dev.fishies.ranim2:core:1.0.0")
        add("implementation", "dev.fishies.ranim2:gui:1.0.0")
        add("ksp", "dev.fishies.ranim2:core:1.0.0")
    }

    tasks.withType<KspAATask>().all {
        outputs.file(jsonFile)
    }
}
