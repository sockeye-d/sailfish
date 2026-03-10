package dev.fishies.ranim2.elements

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import dev.fishies.ranim2.core.CompositeElement
import dev.fishies.ranim2.theming.backgroundColor
import dev.fishies.ranim2.theming.theme
import kotlin.math.roundToInt

typealias TextAnnotation = AnnotatedString.Range<out AnnotatedString.Annotation>

open class TextElement(
    _text: String,
    _fontFamily: FontFamily,
    _position: Offset,
    _fontSize: TextUnit,
    _rotation: Float,
    _color: Color,
    _annotator: (text: String) -> List<TextAnnotation>,
) : BasicElement(_position) {
    var text by mutableStateOf(_text)
    var annotator by mutableStateOf(_annotator)
    var fontSize by mutableStateOf(_fontSize)
    var color by mutableStateOf(_color)
    var rotation by mutableStateOf(_rotation)
    var fontFamily by mutableStateOf(_fontFamily)
    var alignment by mutableStateOf(TextAlign.Left)
    val textLayout by derivedStateOf {
        val constraints = Constraints(
            maxWidth = if (size.width.isNaN()) Int.MAX_VALUE else size.width.roundToInt(),
        )
        measurer.measure(
            AnnotatedString(text, annotator(text)),
            TextStyle(fontSize = this.fontSize, fontFamily = this.fontFamily, textAlign = alignment),
            //overflow = TextOverflow.Ellipsis,
            constraints = constraints,
        )
    }

    override var size by mutableStateOf(Size.Unspecified)
    override val minimumSize by derivedStateOf {
        Size(
            if (size.width.isNaN()) textLayout.size.width.toFloat() else textLayout.multiParagraph.intrinsics.minIntrinsicWidth,
            textLayout.size.height.toFloat()
        )
    }

    override fun DrawScope.draw() {
        withTransform({
            translate(position.x, position.y)
            rotate(rotation, Offset(textLayout.size.width / 2.0f, textLayout.size.height / 2.0f))
        }) {
            drawText(textLayout, color = color)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    companion object {
        private val measurer = TextMeasurer(createFontFamilyResolver(), Density(1f), LayoutDirection.Ltr)
    }
}

fun CompositeElement.makeText(
    text: String,
    fontFamily: FontFamily = FontFamily.Default,
    position: Offset = Offset.Zero,
    fontSize: TextUnit = TextUnit(16f, TextUnitType.Sp),
    rotation: Float = 0f,
    color: Color = theme.contentColorFor(backgroundColor),
    highlighter: (text: String) -> List<TextAnnotation> = { emptyList() },
) = TextElement(text, fontFamily, position, fontSize, rotation, color, highlighter).also { addChild(it) }
