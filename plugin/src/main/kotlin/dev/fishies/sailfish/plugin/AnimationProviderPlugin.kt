package dev.fishies.sailfish.plugin

import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmExtension

const val composeVersion = "1.10.0"
const val kotlinVersion = "2.3.0"

@Suppress("unused")
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
    val resourceDir = projectDir.resolve("src/main/resources")

    extensions.configure<KspExtension> {
        arg("jsonFile", jsonFile.absolutePath)
        arg("jarFile", jarFilePath.absolutePath)
        arg("resourceDir", resourceDir.absolutePath)
    }

    extensions.configure<KotlinJvmExtension> {
        jvmToolchain(21)
    }

    val guiConfig = configurations.create("guiConfig") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    }

    dependencies {
        add("implementation", "dev.fishies.sailfish:core:1.0.0")
        add("guiConfig", "dev.fishies.sailfish:gui:1.0.0")
        add("guiConfig", "dev.fishies.sailfish:core:1.0.0")
        add("ksp", "dev.fishies.sailfish:core:1.0.0")
    }

    tasks.withType<KspAATask>().all {
        outputs.file(jsonFile)
    }

    tasks.withType<Jar>() {
        exclude("**/markers.json")
    }

    // val watchSources = tasks.register("watchSources") {
    //     doLast {
    //         thread(start = true) {
    //             GradleConnector.newConnector()
    //                 .forProjectDirectory(project.projectDir)
    //                 .connect()
    //                 .use { connection: ProjectConnection ->
    //                     connection.newBuild()
    //                         .forTasks("jar")
    //                         .addArguments("--continuous")
    //                         .setStandardInput(System.`in`)
    //                         .setStandardOutput(System.out)
    //                         .run()
    //                 }
    //         }
    //     }
    // }

    val runGui = tasks.register<JavaExec>("runGui") {
        dependsOn(tasks.named("jar"))
        inputs.file(jsonFile)
        // debug = true
        classpath = guiConfig
        mainClass = "dev.fishies.sailfish.gui.MainKt"
        args = listOf(jsonFile.absolutePath)
    }

    tasks.register("gui") {
        dependsOn(runGui)
    }
}
