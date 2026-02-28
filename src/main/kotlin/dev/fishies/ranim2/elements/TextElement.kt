package dev.fishies.ranim2.elements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import dev.fishies.ranim2.core.Scene

open class TextElement(text: String, fontFamily: FontFamily, position: Offset, fontSize: TextUnit, rotation: Float, tint: Color) :
    BasicElement() {
    var text by mutableStateOf(text)
    var position by mutableStateOf(position)
    var fontSize by mutableStateOf(fontSize)
    var color by mutableStateOf(tint)
    var rotation by mutableStateOf(rotation)
    var fontFamily by mutableStateOf(fontFamily)

    override fun DrawScope.draw() {
        val textLayoutResult = measurer.measure(text, TextStyle(fontSize = fontSize * density, fontFamily = fontFamily))
        val textSize = textLayoutResult.size
        val realPosition = position * density
        withTransform({
            translate(realPosition.x, realPosition.y)
            rotate(rotation, Offset(textSize.width / 2.0f, textSize.height / 2.0f))
        }) {
            drawText(textLayoutResult, color = color)
        }
    }

    @OptIn(ExperimentalTextApi::class)
    companion object {
        private val measurer = TextMeasurer(createFontFamilyResolver(), Density(1f), LayoutDirection.Ltr)
    }
}

fun Scene.makeText(
    text: String,
    fontFamily: FontFamily = FontFamily.Default,
    position: Offset = Offset.Zero,
    fontSize: TextUnit = TextUnit(16f, TextUnitType.Sp),
    rotation: Float = 0f,
    color: Color = Color.Unspecified,
) =
    TextElement(text, fontFamily, position, fontSize, rotation, color).also { addChild(it) }
