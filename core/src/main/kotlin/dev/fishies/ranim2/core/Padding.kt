package dev.fishies.ranim2.core

import androidx.compose.ui.geometry.Size

data class Padding(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    constructor(horizontal: Float = 0.0f, vertical: Float = 0.0f) : this(
        left = horizontal, right = horizontal, top = vertical, bottom = vertical
    )

    constructor(all: Float) : this(all, all, all, all)

    companion object {
        val zero = Padding(all = 0.0f)
    }

    val size
        get() = Size(left + right, top + bottom)
}
