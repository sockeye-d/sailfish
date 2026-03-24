package dev.fishies.ranim2.gui.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State

fun <T> mutableStateFrom(immutableState: State<T>, setter: (T) -> Unit) = object : MutableState<T> {
    override var value: T
        get() = immutableState.value
        set(value) = setter(value)

    override fun component1() = immutableState.value

    override fun component2() = setter
}
