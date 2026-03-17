package dev.fishies.ranim2.elements

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.*
import dev.fishies.ranim2.Element
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
