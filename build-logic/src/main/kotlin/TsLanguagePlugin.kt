
import io.github.treesitter.ktreesitter.plugin.GrammarExtension
import io.github.treesitter.ktreesitter.plugin.GrammarFilesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File
import java.util.*

fun <T> Property<T>.assign(value: T) {
    set(value)
}

open class TsParserExtension {
    var name: String = ""
    var module: String = "dev.fishies.sailfish.languages"
}

class TsLanguagePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val ext = target.extensions.create("parser", TsParserExtension::class.java)
        target.generateTreeSitterParser(ext::name, ext::module)
    }
}

private fun Project.generateTreeSitterParser(nameSupplier: () -> String, moduleSupplier: () -> String) {
    plugins.apply("org.jetbrains.kotlin.multiplatform")
    plugins.apply("io.github.tree-sitter.ktreesitter-plugin")

    afterEvaluate {
        val os = OperatingSystem.current()!!
        val name = nameSupplier()
        val module = moduleSupplier()

        val bigLangName = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        val grammarDir = projectDir.resolve("tree-sitter-$name")
        version = grammarDir.resolve("Makefile").readLines().first { it.startsWith("VERSION := ") }
            .removePrefix("VERSION := ")

        val langClassName = "TreeSitter$bigLangName"
        val parserSourceFiles = buildList {
            add(grammarDir.resolve("src/parser.c"))
            grammarDir.resolve("src/scanner.c").let {
                if (it.exists()) add(it)
            }
        }.toTypedArray()

        extensions.configure<GrammarExtension>() {
            baseDir = grammarDir
            interopName = "grammar"
            grammarName = name
            className = langClassName
            libraryName = "ktreesitter-$name"
            packageName = "$module.$name"
            files = parserSourceFiles
        }

        val generateGrammarFilesTask by tasks.named<GrammarFilesTask>("generateGrammarFiles")
        val generatedSrc: Directory by generateGrammarFilesTask.generatedSrc

        extensions.configure<KotlinMultiplatformExtension> {
            jvm()
            jvmToolchain(21)

            sourceSets {
                configureEach {
                    compilerOptions { freeCompilerArgs.add("-Xexpect-actual-classes") }
                    kotlin.srcDir(generatedSrc.dir(this.name).dir("kotlin"))
                }
                jvmMain {
                    resources.srcDir(generatedSrc.dir("jvmMain").dir("resources"))
                    dependencies {
                        implementation("io.github.tree-sitter:ktreesitter:0.24.1")
                        api(project(":languages:common"))
                    }
                }
            }
        }

        val makeLangInclude = tasks.register("makeLangInclude") {
            dependsOn(generateGrammarFilesTask)

            val inputFile = grammarDir.run {
                resolveOrNull("bindings/c/tree-sitter.h.in") ?: resolveOrNull("bindings/c/tree-sitter-$name.h")
                ?: error("No C bindings in $grammarDir found!")
            }
            val outputFile = generatedSrc.file("jni/tree_sitter/tree-sitter-$name.h")
            inputs.file(inputFile)
            outputs.file(outputFile)

            doLast {
                outputFile.asFile.parentFile.mkdirs()
                outputFile.asFile.writeText(
                    inputFile.readText().replace("@UPPER_PARSERNAME@", name.uppercase()).replace("@PARSERNAME@", name)
                )
            }
        }

        val compileGrammarTask = tasks.register("compileGrammar") {
            dependsOn(makeLangInclude)

            val libPath = getLibPathFor(os, System.getProperty("os.arch"), os.getSharedLibraryName("ktreesitter-$name"))

            val libFile = generatedSrc.dir("jvmMain/resources/").file(libPath)
            libFile.asFile.parentFile.mkdirs()
            val grammarSrcDir = grammarDir.resolve("src")
            val jniDir = generatedSrc.dir("jni")

            inputs.dir(grammarSrcDir)
            inputs.property("java.home", System.getProperty("java.home"))
            inputs.property("os.name", System.getProperty("os.name"))
            inputs.property("os.arch", System.getProperty("os.arch"))
            outputs.file(libFile)
            outputs.cacheIf { true }

            doLast {
                exec {
                    val cc = if (os.isMacOsX) "clang" else "gcc"

                    commandLine(
                        cc, "-shared", "-fPIC", "-O2", "-std=c11",
                        "-I$grammarSrcDir",
                        "-I${jniDir.file("tree_sitter")}",
                        "-I${System.getProperty("java.home")}/include/${System.getProperty("os.name").toLowerCase()}",
                        "-I${System.getProperty("java.home")}/include",
                        "-o", libFile,
                        *parserSourceFiles,
                        jniDir.file("binding.c"),
                    )
                }
            }
        }

        val baseInterface = "dev.fishies.sailfish.languages.common.TreeSitterLanguage"
        val knownQueries = mapOf(
            "highlights" to "dev.fishies.sailfish.languages.common.TreeSitterLanguage.Highlightable",
            "tags" to "dev.fishies.sailfish.languages.common.TreeSitterLanguage.Taggable",
        )

        val modifyGeneratedFileTask = tasks.register("modifyGeneratedFile") {
            dependsOn(generateGrammarFilesTask)

            val queriesFolder = grammarDir.resolve("queries/")
            if (!queriesFolder.exists()) {
                return@register
            }

            val folder = generatedSrc.dir("jvmMain/kotlin/${module.replace(".", "/")}/$name")
            val generatedFile = folder.file("$langClassName.kt")
            val files = queriesFolder.listFiles()

            inputs.file(generatedFile)
            outputs.file(generatedFile)

            doLast {
                val text = generatedFile.asFile.readText()
                val implString = buildString {
                    for (query: File in files) {
                        val rawString = "\"\"\"${query.readText().escapeString()}\"\"\""
                        val queryName = query.nameWithoutExtension

                        if (queryName in knownQueries) {
                            appendLine("override val $queryName = $rawString")
                        } else {
                            appendLine("const val $queryName = $rawString")
                        }
                    }
                }
                val baseInterfaces = files.mapNotNull { knownQueries[it.nameWithoutExtension] } + baseInterface
                val newText = text.replace(
                    "actual object $langClassName {",
                    "actual object $langClassName : ${baseInterfaces.joinToString()} {\n$implString\n",
                ).replace("actual fun language()", "actual override fun language()")
                generatedFile.asFile.writeText(newText)
            }
        }

        tasks.named("jvmProcessResources") { dependsOn(compileGrammarTask) }
        tasks.named("compileKotlinJvm") {
            dependsOn(generateGrammarFilesTask)
            dependsOn(modifyGeneratedFileTask)
        }
    }
}

private fun String.escapeString() = replace("\${", "\${\"$\"}{").replace("\"\"\"", "$" + """{"\"\"\""}""")

private fun getLibPathFor(os: OperatingSystem, archName: String, libName: String): String {
    val osName = when {
        os.isLinux -> "linux"
        os.isMacOsX -> "macos"
        os.isWindows -> "windows"
        else -> error("Unsupported operating system: $os")
    }

    val arch = when {
        "amd64" in archName || "x86_64" in archName -> "x64"
        "aarch64" in archName || "arm64" in archName -> "aarch64"
        else -> error("Unsupported architecture: $archName")
    }
    return "lib/$osName/$arch/$libName"
}

private fun File.resolveOrNull(path: String) = resolve(path).takeIf { it.exists() }
