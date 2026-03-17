@file:UseSerializers(Theme.ColorSerializer::class)

package dev.fishies.ranim2.theming

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.fishies.ranim2.Element
import dev.fishies.ranim2.attached
import dev.fishies.ranim2.core.*
import dev.fishies.ranim2.util.fromHtmlColor
import dev.fishies.ranim2.util.toHtmlColor
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KProperty1

enum class ThemeColor(
    internal val bgColor: KProperty1<Theme, Color>, internal val contentColor: KProperty1<Theme, Color>
) {
    Primary(Theme::primary, Theme::onPrimary),
    PrimaryVariant(Theme::primaryVariant, Theme::onPrimary),
    Secondary(Theme::secondary, Theme::onSecondary),
    SecondaryVariant(Theme::secondaryVariant, Theme::onSecondary),
    Background(Theme::background, Theme::onBackground),
    Background2(Theme::background2, Theme::onBackground),
    Surface(Theme::surface, Theme::onSurface),
    Error(Theme::error, Theme::onError),
}

@Serializable
data class Theme(
    val primary: Color,
    @SerialName("primary_variant") val primaryVariant: Color,
    val secondary: Color,
    @SerialName("secondary_variant") val secondaryVariant: Color,
    val background: Color,
    val background2: Color,
    val surface: Color,
    val error: Color,
    @SerialName("on_primary") val onPrimary: Color,
    @SerialName("on_secondary") val onSecondary: Color,
    @SerialName("on_background") val onBackground: Color,
    @SerialName("on_surface") val onSurface: Color,
    @SerialName("on_error") val onError: Color,
    @SerialName("is_light") val isLight: Boolean,
    val syntax: SyntaxHighlighterTheme,
) {
    object ColorSerializer : KSerializer<Color> {
        override val descriptor: SerialDescriptor
            get() = PrimitiveSerialDescriptor("dev.fishies.ranim2.theming.Theme.ColorSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Color) {
            encoder.encodeString(value.toHtmlColor())
        }

        override fun deserialize(decoder: Decoder): Color {
            return decoder.decodeString().fromHtmlColor()
        }
    }

    operator fun get(color: ThemeColor) = color.bgColor(this)

    fun backgroundColorFor(contentColor: ThemeColor) = contentColor.bgColor(this)
    fun contentColorFor(background: ThemeColor) = background.contentColor(this)
}

@Serializable
data class SyntaxStyle(
    val color: Color,
    @SerialName("font_style") val fontStyle: String? = null,
    @SerialName("font_weight") val fontWeight: Int? = null,
) {
    fun toSpanStyle() = SpanStyle(
        color = color, fontStyle = when (fontStyle) {
            "normal" -> FontStyle.Normal
            "italic", "oblique" -> FontStyle.Italic
            else -> null
        }, fontWeight = fontWeight?.let(::FontWeight)
    )
}

@Serializable(SyntaxHighlighterTheme.Serializer::class)
data class SyntaxHighlighterTheme(
    val syntax: Map<String, SyntaxStyle>,
) {
    operator fun get(name: String): SpanStyle {
        var name = name
        repeat(name.count { it == '.' }) {
            syntax[name]?.toSpanStyle()?.let { return it }
            name = name.substringBeforeLast('.')
        }
        return syntax["text"]?.toSpanStyle() ?: SpanStyle(color = Color.Blue)
    }

    internal object Serializer : KSerializer<SyntaxHighlighterTheme> {
        val delegate = MapSerializer(String.serializer(), SyntaxStyle.serializer())
        override val descriptor: SerialDescriptor = delegate.descriptor

        override fun serialize(
            encoder: Encoder,
            value: SyntaxHighlighterTheme,
        ) = delegate.serialize(encoder, value.syntax)

        override fun deserialize(decoder: Decoder) = SyntaxHighlighterTheme(syntax = delegate.deserialize(decoder))
    }
}

val defaultTheme = Theme(
    primary = Color(0xFFFFFF86),
    primaryVariant = Color(0xFFFFDB77),
    secondary = Color(0xFF53D3FF),
    secondaryVariant = Color(0xFF5278FF),
    background = Color(0xFF0F0F0F),
    background2 = Color(0xFF181818),
    surface = Color(0xFF2F2F2F),
    error = Color(0xFFFF7D68),
    onPrimary = Color(0xFF0F0F0F),
    onSecondary = Color(0xFF0F0F0F),
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFF0F0F0),
    onError = Color(0xFF0F0F0F),
    isLight = false,
    syntax = SyntaxHighlighterTheme(
        syntax = mapOf("text" to SyntaxStyle(Color.White))
    )
)

internal class ThemeProperties {
    var theme by mutableStateOf(defaultTheme)
}

internal class BackgroundProperties {
    var backgroundColor by mutableStateOf(ThemeColor.Background)
}

var Element.theme by attached<_, _, Element?>(ThemeProperties::theme, recursive = true) { defaultTheme }
var Element.backgroundColor by attached<_, _, Element?>(
    BackgroundProperties::backgroundColor, recursive = true
) { ThemeColor.Background }
