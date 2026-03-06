package dev.fishies.ranim2.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope

interface Element {
    var parent: Element?
    var position: Offset
    var size: Size
    val minimumSize: Size
    var visible: Boolean
    fun DrawScope.draw()

    fun runLayoutPass()
}
