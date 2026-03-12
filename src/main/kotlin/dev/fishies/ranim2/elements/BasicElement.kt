package dev.fishies.ranim2.elements

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import dev.fishies.ranim2.core.Element
import dev.fishies.ranim2.core.coerceAtLeast
import kotlin.reflect.KClass

abstract class BasicElement(position: Offset, size: Size = Size.Unspecified) : Element {
    override var attachedProperties by mutableStateOf(emptyMap<KClass<*>, Any?>())
    override var children by mutableStateOf(emptyList<Element>())

    override var parent: Element? by mutableStateOf(null)
    override fun runLayoutPass() {}

    override var position by mutableStateOf(position)
    override var size by mutableStateOf(size)
    override var visible by mutableStateOf(true)

    private val initialSize = size
    override val minimumSize by derivedStateOf { if (customMinimumSize.isSpecified) customMinimumSize else initialSize }

    override fun toString() = stringRepresentation

    var customMinimumSize by mutableStateOf(Size.Unspecified)
}
