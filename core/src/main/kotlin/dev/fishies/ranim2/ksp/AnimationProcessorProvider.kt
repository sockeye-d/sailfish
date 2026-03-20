package dev.fishies.ranim2.ksp

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import dev.fishies.ranim2.AnimationProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AnimationMetadata(
    val jarFileOutputPath: String,
    val animations: List<AnimationSymbol>,
)

@Serializable
data class AnimationSymbol(
    val ownerClassName: String,
    val fnName: String,
    val signature: String,
    val data: Data,
) {
    override fun toString() = "$ownerClassName.$fnName($signature)"

    @Serializable
    data class Data(
        val length: Int = Int.MAX_VALUE,
    )
}

class AnimationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment) = AnimationProviderProcessor(environment)
}

class AnimationProviderProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private val json = Json {
        prettyPrint = true
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val jsonFile = File(environment.options["jsonFile"] ?: error("JSON file path required"))
        val annotated = resolver.getSymbolsWithAnnotation(AnimationProvider::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()
        val animations = annotated.map {
            AnimationSymbol(
                ownerClassName = resolver.getOwnerJvmClassName(it)!!,
                fnName = resolver.getJvmName(it)!!,
                signature = resolver.mapToJvmSignature(it)!!,
                data = AnimationSymbol.Data(
                    length = it.getAnnotationsByType(AnimationProvider::class).first().length,
                )
            )
        }
        jsonFile.writeText(
            json.encodeToString(
                AnimationMetadata(
                    jarFileOutputPath = environment.options["jarFile"] ?: error("JAR filepath required"),
                    animations = animations.toList(),
                )
            )
        )
        return emptyList()
    }
}
