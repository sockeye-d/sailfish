package dev.fishies.ranim2.elements

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import dev.fishies.ranim2.core.Element

abstract class BasicElement(position: Offset, size: Size = Size.Unspecified) : Element {
    override var parent: Element? by mutableStateOf(null)
    override fun runLayoutPass() {}

    override var position by mutableStateOf(position)
    override var size by mutableStateOf(size)
    override var visible by mutableStateOf(true)

    override val minimumSize by derivedStateOf { size }
}
