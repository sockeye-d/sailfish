import org.gradle.kotlin.dsl.support.uppercaseFirstChar

val langPackage = "dev.fishies.ranim2.languages"
val langName = "kotlin"

val bigLangName = langName.uppercaseFirstChar()
val grammarDir = projectDir.resolve("tree-sitter-$langName")

version = grammarDir.resolve("Makefile").readLines().first { it.startsWith("VERSION := ") }.removePrefix("VERSION := ")

plugins {
    kotlin("multiplatform")
    id("io.github.tree-sitter.ktreesitter-plugin")
}

grammar {
    baseDir = grammarDir
    interopName = "grammar"
    grammarName = langName
    className = "TreeSitter$bigLangName"
    libraryName = "ktreesitter-$langName"
    packageName = "$langPackage.$langName"
    files = arrayOf(
        grammarDir.resolve("src/scanner.c"),
        grammarDir.resolve("src/parser.c"),
    )
}

val generateGrammarFilesTask = tasks.generateGrammarFiles.get()

kotlin {
    jvm()
    jvmToolchain(21)

    sourceSets {
        val generatedSrc = generateGrammarFilesTask.generatedSrc.get()
        configureEach {
            kotlin.srcDir(generatedSrc.dir(name).dir("kotlin"))
            compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
        }
        jvmMain {
            resources.srcDir(generatedSrc.dir("jvmMain").dir("resources"))
            dependencies {
                implementation(libs.treesitter)
            }
        }
    }
}

val makeLangInclude = tasks.register<Copy>("makeLangInclude") {
    dependsOn(generateGrammarFilesTask)
    from(grammarDir.resolve("bindings/c/tree-sitter.h.in")) {
        filter {
            it.replace("@UPPER_PARSERNAME@", langName.uppercase()).replace("@PARSERNAME@", langName)
        }
    }

    into(generateGrammarFilesTask.generatedSrc.get().file("jni/tree_sitter/tree-sitter-$langName.h"))
}

val compileGrammarTask = tasks.register<Exec>("compileGrammar") {
    dependsOn(generateGrammarFilesTask)
    dependsOn(makeLangInclude)

    val outDir = generateGrammarFilesTask.generatedSrc.get().dir("jvmMain/resources/lib/linux/x64")
    outDir.asFile.mkdirs()

    val libFile = outDir.file("libktreesitter-$langName.so")
    val grammarSrcDir = grammarDir.resolve("src")
    val cBindingsDir = grammarDir.resolve("bindings/c")

    inputs.dir(grammarSrcDir)
    inputs.dir(cBindingsDir)
    outputs.file(libFile)

    commandLine(
        "gcc", "-shared", "-fPIC", "-O2", "-std=c11",
        "-I$grammarSrcDir",
        "-I$cBindingsDir",
        "-I${cBindingsDir.resolve("tree_sitter")}",
        "-I${System.getProperty("java.home")}/include",
        "-I${System.getProperty("java.home")}/include/linux",
        "-o", libFile,
        grammarDir.resolve("src/parser.c"),
        grammarDir.resolve("src/scanner.c"),
        generateGrammarFilesTask.generatedSrc.get().file("jni/binding.c"),
    )

    logging.captureStandardOutput(LogLevel.ERROR)
}

tasks.named("jvmProcessResources") { dependsOn(compileGrammarTask) }
tasks.named("compileKotlinJvm") { dependsOn(generateGrammarFilesTask) }
