package dev.fishies.ranim2.plugin

import com.google.devtools.ksp.gradle.KspAATask
import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
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

    extensions.configure<KspExtension> {
        arg("jsonFile", jsonFile.absolutePath)
        arg("jarFile", jarFilePath.absolutePath)
    }

    extensions.configure<KotlinJvmExtension> {
        jvmToolchain(21)
    }

    val guiConfig = configurations.create("guiConfig") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    dependencies {
        add("implementation", "org.jetbrains.compose.components:components-resources:$composeVersion")
        add("implementation", "dev.fishies.ranim2:core:1.0.0")
        add("guiConfig", "dev.fishies.ranim2:gui:1.0.0")
        add("guiConfig", "dev.fishies.ranim2:core:1.0.0")
        add("ksp", "dev.fishies.ranim2:core:1.0.0")
    }

    tasks.withType<KspAATask>().all {
        outputs.file(jsonFile)
    }

    val watchSources = tasks.register("watchSources") {
        doLast {
            GradleConnector.newConnector()
                .forProjectDirectory(project.projectDir)
                .connect()
                .use { connection: ProjectConnection ->
                    connection.newBuild()
                        .forTasks("jar")
                        .addArguments("--continuous")
                        .setStandardInput(System.`in`)
                        .setStandardOutput(System.out)
                        .run()
                }
        }
    }

    // val runGui = tasks.register<JavaExec>("runGui") {
    //     dependsOn(tasks.named("jar"))
    //     inputs.file(jsonFile)
    //     // println(ManagementFactory.getRuntimeMXBean().inputArguments)
    //     debug = true
    //     args.addAll(ManagementFactory.getRuntimeMXBean().inputArguments)
    //     classpath = guiConfig
    //     mainClass = "dev.fishies.ranim2.gui.MainKt"
    //     args = listOf(jsonFile.absolutePath)
    // }
    val runGui = tasks.register<JavaExec>("runGui") {
        dependsOn(tasks.named("jar"))
        inputs.file(jsonFile)
        debug = true
        classpath = guiConfig
        mainClass = "dev.fishies.ranim2.gui.MainKt"
        args = listOf(jsonFile.absolutePath)
    }

    tasks.register("gui") {
        dependsOn(runGui)
    }
}
