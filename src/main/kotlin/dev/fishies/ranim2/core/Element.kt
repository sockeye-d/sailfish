package dev.fishies.ranim2.core

import androidx.compose.ui.graphics.drawscope.DrawScope

interface Element {
    var visible: Boolean
    fun DrawScope.draw()
}
