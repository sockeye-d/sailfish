package dev.fishies.ranim2.elements

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.fishies.ranim2.core.Element

abstract class BasicElement : Element {
    override var visible by mutableStateOf(true)
}
