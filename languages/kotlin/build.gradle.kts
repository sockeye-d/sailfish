import io.github.treesitter.ktreesitter.plugin.GrammarFilesTask
import java.io.OutputStream.nullOutputStream
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.support.useToRun
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.tasks.CInteropProcess
import org.jetbrains.kotlin.konan.target.PlatformManager

inline val File.unixPath: String
    get() = if (!os.isWindows) path else path.replace("\\", "/")

val grammarDir = projectDir.resolve("tree-sitter-kotlin")
val os: OperatingSystem = OperatingSystem.current()
val libsDir = layout.buildDirectory.get().dir("libs")

version = grammarDir.resolve("Makefile").readLines()
    .first { it.startsWith("VERSION := ") }.removePrefix("VERSION := ")

plugins {
    kotlin("multiplatform")
    id("io.github.tree-sitter.ktreesitter-plugin")
}

grammar {
    baseDir = grammarDir
    interopName = "grammar"
    grammarName = "kotlin"
    className = "TreeSitterKotlin"
    libraryName = "ktreesitter-kotlin"
    packageName = "dev.fishies.ranim2.languages.kotlin"
    files = arrayOf(
        grammarDir.resolve("src/scanner.c"),
        grammarDir.resolve("src/parser.c")
    )
}

val generateTask: GrammarFilesTask = tasks.generateGrammarFiles.get()

kotlin {
    jvm()
    jvmToolchain(21)

    sourceSets {
        val generatedSrc = generateTask.generatedSrc.get()
        configureEach {
            kotlin.srcDir(generatedSrc.dir(name).dir("kotlin"))
            compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
        }
        jvmMain {
            resources.srcDir(generatedSrc.dir("jvmMain").dir("resources"))
            dependencies {
                implementation("io.github.tree-sitter:ktreesitter:0.24.1")
            }
        }
    }
}

val compileGrammarTask = tasks.register<Exec>("compileGrammar") {
    dependsOn(generateTask)

    val generatedSrc = generateTask.generatedSrc.get()
    val out = generatedSrc.dir("jvmMain").dir("resources").dir("lib/linux/x64").also { it.asFile.mkdirs() }
    val libFile = out.file("libktreesitter-kotlin.so")
    logger.log(LogLevel.ERROR, "$libFile")
    outputs.file(libFile)

    commandLine(
        "gcc", "-shared", "-fPIC", "-O0", "-std=c11",
        "-I${grammarDir.resolve("src")}",
        "-I${grammarDir.resolve("bindings/c")}",
        "-I${System.getProperty("java.home")}/include",
        "-I${System.getProperty("java.home")}/include/linux",
        "-o", libFile,
        grammarDir.resolve("src/parser.c"),
        grammarDir.resolve("src/scanner.c"),
        generatedSrc.file("jni/binding.c"),
    )

    logging.captureStandardOutput(LogLevel.ERROR)
}

tasks.named("jvmProcessResources") { dependsOn(compileGrammarTask) }
tasks.named("compileKotlinJvm") { dependsOn(generateTask) }

/*
grammar {
    baseDir = grammarDir
    interopName = "grammar"
    grammarName = "kotlin"
    className = "TreeSitterKotlin"
    libraryName = "ktreesitter-kotlin"
    packageName = "dev.fishies.ranim2.languages.kotlin"
    files = arrayOf(
        grammarDir.resolve("src/scanner.c"),
        grammarDir.resolve("src/parser.c")
    )
}

val generateTask: GrammarFilesTask = tasks.generateGrammarFiles.get()

kotlin {
    jvm {}

    //androidTarget {
    //    withSourcesJar(true)
    //    publishLibraryVariants("release")
    //}

    when {
        os.isLinux -> listOf(linuxX64(), linuxArm64())
        os.isWindows -> listOf(mingwX64())
        os.isMacOsX -> listOf(
            macosArm64(),
            macosX64(),
            iosArm64(),
            iosSimulatorArm64()
        )

        else -> {
            val arch = System.getProperty("os.arch")
            throw GradleException("Unsupported platform: $os ($arch)")
        }
    }.forEach { target ->
        target.compilations.configureEach {
            cinterops.create(grammar.interopName.get()) {
                definitionFile.set(generateTask.interopFile.asFile.get())
                includeDirs.allHeaders(grammarDir.resolve("bindings/c"))
                extraOpts("-libraryPath", libsDir.dir(konanTarget.name))
                tasks.getByName(interopProcessingTaskName).mustRunAfter(generateTask)
            }
        }
    }

    jvmToolchain(21)

    sourceSets {
        val generatedSrc = generateTask.generatedSrc.get()
        configureEach {
            kotlin.srcDir(generatedSrc.dir(name).dir("kotlin"))
        }

        commonMain {
            languageSettings {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }

            dependencies {

            }
        }

        jvmMain {
            resources.srcDir(generatedSrc.dir(name).dir("resources"))
        }
    }
}

@Suppress("DEPRECATION")
tasks.withType<CInteropProcess>().configureEach {
    if (name.startsWith("cinteropTest")) return@configureEach
    dependsOn(tasks.named("generateGrammarFiles"))

    val grammarFiles = grammar.files.get()
    val grammarName = grammar.grammarName.get()
    val runKonan = File(konanHome.get()).resolve("bin")
        .resolve(if (os.isWindows) "run_konan.bat" else "run_konan").path
    val libFile = libsDir.dir(konanTarget.name).file("libtreesitter-$grammarName.a").asFile
    val objectFiles = grammarFiles.map {
        grammarDir.resolve(it.nameWithoutExtension + ".o").path
    }.toTypedArray()
    val loader = PlatformManager(konanHome.get(), false, konanDataDir.orNull).loader(konanTarget)

    doFirst {
        if (!File(loader.absoluteTargetToolchain).isDirectory) loader.downloadDependencies()

        val argsFile = File.createTempFile("args", null)
        argsFile.deleteOnExit()
        logger.log(LogLevel.ERROR, grammarDir.resolve("src").unixPath)
        argsFile.writer().useToRun {
            write("-I" + grammarDir.resolve("src").unixPath + "\n")
            write("-I" + grammarDir.unixPath + "\n")
            write("-DTREE_SITTER_HIDE_SYMBOLS\n")
            write("-fvisibility=hidden\n")
            write("-std=c11\n")
            write("-O2\n")
            write("-g\n")
            write("-c\n")
            grammarFiles.forEach { write(it.unixPath + "\n") }
        }

        exec {
            executable = runKonan
            workingDir = grammarDir
            standardOutput = nullOutputStream()
            args("clang", "clang", konanTarget.name, "@" + argsFile.path)
        }

        exec {
            executable = runKonan
            workingDir = grammarDir
            standardOutput = nullOutputStream()
            args("llvm", "llvm-ar", "rcs", libFile.path, *objectFiles)
        }
    }

    inputs.files(*grammarFiles)
    outputs.file(libFile)
}

/*
val generateTask = tasks.generateGrammarFiles.get()

kotlin {
    jvm {}

    when {
        os.isLinux -> listOf(linuxX64())
        //os.isWindows -> listOf(mingwX64())
        //os.isMacOsX -> listOf(
        //    macosArm64(),
        //    macosX64(),
        //    iosArm64(),
        //    iosSimulatorArm64()
        //)
        else -> {
            val arch = System.getProperty("os.arch")
            throw GradleException("Unsupported platform: $os ($arch)")
        }
    }.forEach { target ->
        target.compilations.configureEach {
            cinterops.create(grammar.interopName.get()) {
                definitionFile.set(generateTask.interopFile.asFile.get())
                includeDirs.allHeaders(grammarDir.resolve("bindings/c"))
                extraOpts("-libraryPath", libsDir.dir(konanTarget.name))
                tasks.getByName(interopProcessingTaskName).mustRunAfter(generateTask)
            }
        }
    }

    jvmToolchain(17)

    sourceSets {
        val generatedSrc = generateTask.generatedSrc.get()
        configureEach {
            kotlin.srcDir(generatedSrc.dir(name).dir("kotlin"))
        }

        commonMain {
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            languageSettings {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }

            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }

        jvmMain {
            resources.srcDir(generatedSrc.dir(name).dir("resources"))
        }
    }
}

tasks.withType<CInteropProcess>().configureEach {
    if (name.startsWith("cinteropTest")) return@configureEach

    val grammarFiles = grammar.files.get()
    val grammarName = grammar.grammarName.get()
    val runKonan = File(konanHome.get()).resolve("bin")
        .resolve(if (os.isWindows) "run_konan.bat" else "run_konan").path
    val libFile = libsDir.dir(konanTarget.name).file("libtreesitter-$grammarName.a").asFile
    val objectFiles = grammarFiles.map {
        grammarDir.resolve(it.nameWithoutExtension + ".o").path
    }.toTypedArray()
    val loader = PlatformManager(konanHome.get(), false, konanDataDir.orNull).loader(konanTarget)

    doFirst {
        if (!File(loader.absoluteTargetToolchain).isDirectory) loader.downloadDependencies()

        val argsFile = File.createTempFile("args", null)
        argsFile.deleteOnExit()
        argsFile.writer().useToRun {
            write("-I" + grammarDir.resolve("src").unixPath + "\n")
            write("-DTREE_SITTER_HIDE_SYMBOLS\n")
            write("-fvisibility=hidden\n")
            write("-std=c11\n")
            write("-O2\n")
            write("-g\n")
            write("-c\n")
            grammarFiles.forEach { write(it.unixPath + "\n") }
        }

        exec {
            executable = runKonan
            workingDir = grammarDir
            standardOutput = nullOutputStream()
            args("clang", "clang", konanTarget.name, "@" + argsFile.path)
        }

        exec {
            executable = runKonan
            workingDir = grammarDir
            standardOutput = nullOutputStream()
            args("llvm", "llvm-ar", "rcs", libFile.path, *objectFiles)
        }
    }

    inputs.files(*grammarFiles)
    outputs.file(libFile)
}

//kotlin {
//    jvm()
//    jvmToolchain(21)
//
//    sourceSets {
//        val generatedSrc = generateTask.generatedSrc.get()
//        configureEach {
//            kotlin.srcDir(generatedSrc.dir(name).dir("kotlin"))
//        }
//        jvmMain {
//            resources.srcDir(generatedSrc.dir("jvmMain").dir("resources"))
//            dependencies {
//                implementation("io.github.tree-sitter:ktreesitter:0.24.1")
//            }
//        }
//    }
//}
//
//tasks.register<Exec>("compileGrammar") {
//    val generatedSrc = generateTask.generatedSrc.get()
//    val out = generatedSrc.dir("jvmMain").dir("resources").dir("lib/linux/x64").also { it.asFile.mkdirs() }
//    val libFile = out.file("libktreesitter-kotlin.so")
//    logger.log(LogLevel.ERROR, "$libFile")
//    outputs.file(libFile)
//
//    commandLine(
//        "gcc", "-shared", "-fPIC", "-O0", "-std=c11",
//        "-I${grammarDir.resolve("src")}",
//        "-I${grammarDir.resolve("bindings/c")}",
//        "-I${System.getProperty("java.home")}/include",
//        "-I${System.getProperty("java.home")}/include/linux",
//        "-o", libFile,
//        grammarDir.resolve("src/parser.c"),
//        grammarDir.resolve("src/scanner.c"),
//        generatedSrc.file("jni/binding.c"),
//    )
//
//    logging.captureStandardOutput(LogLevel.ERROR)
//}
//
//tasks.named("jvmProcessResources") { dependsOn("compileGrammar") }
//tasks.named("jvmProcessResources") { dependsOn("generateGrammarFiles") }
//tasks.named("compileKotlinJvm") { dependsOn(generateTask) }
