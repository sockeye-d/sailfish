package dev.fishies.ranim2.elements

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.*
import dev.fishies.ranim2.core.Scene

class ShapeElement(shape: Shape, position: Offset, size: Size, rotation: Float, color: Color, style: DrawStyle) :
    BasicElement() {
    var position by mutableStateOf(position)
    var size by mutableStateOf(size)
    var color by mutableStateOf(color)
    var rotation by mutableStateOf(rotation)
    var style by mutableStateOf(style)
    var shape by mutableStateOf(shape)

    override fun DrawScope.draw() {
        val realSize = this@ShapeElement.size * density
        val realPosition = position * density
        val shapeOutline = shape.createOutline(realSize, LayoutDirection.Ltr, Density(density))
        withTransform({
            translate(realPosition.x, realPosition.y)
            rotate(rotation, Offset(realSize.width / 2, realSize.height / 2))
        }) {
            drawOutline(shapeOutline, color, style = style)
        }
    }
}

fun Scene.makeShape(
    shape: Shape,
    size: Size,
    color: Color,
    style: DrawStyle = Fill,
    position: Offset = Offset.Zero,
    rotation: Float = 0f,
) = ShapeElement(shape, position, size, rotation, color, style).also { addChild(it) }

fun Scene.makeCircle(
    size: Size,
    color: Color,
    style: DrawStyle = Fill,
    position: Offset = Offset.Zero,
    rotation: Float = 0f,
) = makeShape(CircleShape, size, color, style, position, rotation)

fun Scene.makeRectangle(
    size: Size,
    color: Color,
    style: DrawStyle = Fill,
    position: Offset = Offset.Zero,
    radius: Float = 0f,
    rotation: Float = 0f,
) = makeShape(RoundedCornerShape(radius), size, color, style, position, rotation)
