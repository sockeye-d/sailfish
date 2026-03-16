package dev.fishies.ranim2.syntax

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import dev.fishies.ranim2.languages.common.TreeSitterLanguage
import dev.fishies.ranim2.theming.SyntaxHighlighterTheme
import io.github.treesitter.ktreesitter.*
import java.util.concurrent.ConcurrentHashMap

private val languageCache = ConcurrentHashMap<String, Language>()
private val parserCache = ConcurrentHashMap<String, Parser>()
private val hlQueryCache = ConcurrentHashMap<String, Query>()

private val Any.qualifiedName
    get() = this::class.qualifiedName ?: javaClass.toString()

fun TreeSitterLanguage.makeLanguage(): Language = languageCache.getOrPut(qualifiedName) { Language(language()) }

fun TreeSitterLanguage.makeParser(): Parser = parserCache.getOrPut(qualifiedName) { Parser(makeLanguage()) }

fun TreeSitterLanguage.Highlightable.highlight(text: String) =
    hlQueryCache.getOrPut(qualifiedName) { Query(makeLanguage(), highlights) }
        .matches(makeParser().parse(text).rootNode)

/**
 * @param byteToIndex Tree-sitter returns a UTF-8 byte offset, but JVM strings are indexed by UTF-16 code points.
 * Therefore, something needs to convert between the two for proper handling of CJK strings.
 */
fun Sequence<QueryMatch>.toAnnotations(
    byteToIndex: UInt.() -> Int, theme: SyntaxHighlighterTheme
) = mapNotNull { q ->
    q.captures.firstOrNull()?.let {
        AnnotatedString.Range(theme[it.name], it.node.startByte.byteToIndex(), it.node.endByte.byteToIndex())
    }
}.sortedBy { it.start }

fun TreeSitterLanguage.Highlightable.highlightToAnnotations(
    text: String, theme: SyntaxHighlighterTheme
): List<AnnotatedString.Range<SpanStyle>> {
    val textBytes = text.toByteArray(Charsets.UTF_8)
    return hlQueryCache.getOrPut(qualifiedName) { Query(makeLanguage(), highlights) }
        .matches(makeParser().parse(text).rootNode)
        .toAnnotations({ textBytes.sliceArray(0..<toInt()).toString(Charsets.UTF_8).length }, theme)
        .toList()
}
