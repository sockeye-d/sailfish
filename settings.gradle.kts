enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("io.github.tree-sitter.ktreesitter-plugin") version "0.24.1"
        id("com.android.library") version "8.13.2"
        id("org.jetbrains.kotlin.jvm") version "2.3.0"
        id("org.jetbrains.kotlin.multiplatform") version "2.3.0"
    }
}

dependencyResolutionManagement {
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
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "sailfish"

includeBuild("build-logic")
includeBuild("plugin")
include(":core")
include(":gui")

val coreSelector = project(":core")

includeBuild("test")

for (project in rootDir.resolve("languages").list()) {
    include(":languages:$project")
}
