package dev.fishies.ranim2.elements

import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.*
import dev.fishies.ranim2.core.CompositeElement

typealias TextAnnotation = AnnotatedString.Range<out AnnotatedString.Annotation>

open class TextElement(
    text: String,
    fontFamily: FontFamily,
    position: Offset,
    fontSize: TextUnit,
    rotation: Float,
    tint: Color,
    annotations: List<TextAnnotation>,
) : BasicElement(position) {
    private operator fun State<Size>.setValue(
        thisObj: Any?,
        property: Any?,
        value: Any?,
    ): Unit = error("Cannot set a TextElement's size, as it is calculated intrinsically!")

    var text by mutableStateOf(text)
    var annotations by mutableStateOf(annotations)
    var fontSize by mutableStateOf(fontSize)
    var color by mutableStateOf(tint)
    var rotation by mutableStateOf(rotation)
    var fontFamily by mutableStateOf(fontFamily)
    val textLayout by derivedStateOf {
        measurer.measure(
            AnnotatedString(this.text, this.annotations),
            TextStyle(fontSize = this.fontSize, fontFamily = this.fontFamily)
        )
    }

    override var size by derivedStateOf { this.textLayout.size.toSize() }

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
    color: Color = Color.Unspecified,
    annotations: List<TextAnnotation> = emptyList(),
) = TextElement(text, fontFamily, position, fontSize, rotation, color, annotations).also { addChild(it) }
