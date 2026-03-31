package dev.fishies.sailfish.gui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.takeOrElse
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
suspend fun PointerInputScope.detectDragGesturesAbsolute(
    matcher: PointerMatcher = PointerMatcher.Primary, initialOffset: () -> Offset, onDragEnd: () -> Unit, onDrag: (Offset, absoluteAbsolute: Offset) -> Unit
) {
    var absoluteDragPosition = Offset.Unspecified
    var absoluteAbsoluteDragPosition = Offset.Unspecified
    detectDragGestures(matcher, onDragStart = {
        absoluteAbsoluteDragPosition = it + initialOffset()
    }, onDragEnd = {
        absoluteDragPosition = Offset.Unspecified
        absoluteAbsoluteDragPosition = Offset.Unspecified
        onDragEnd()
    }, onDrag = {
        absoluteDragPosition = absoluteDragPosition.takeOrElse { initialOffset() } + it
        absoluteAbsoluteDragPosition += it
        onDrag(absoluteDragPosition, absoluteAbsoluteDragPosition)
    })
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.onDragAbsolute(matcher: PointerMatcher = PointerMatcher.Primary, vararg keys: Any?, initialOffset: () -> Offset, onDragEnd: () -> Unit = {}, onDrag: (Offset, absoluteAbsolute: Offset) -> Unit) = composed(
    inspectorInfo = {
        name = "onDragAbsolute"
        properties["matcher"] = matcher
        properties["onDrag"] = onDrag
    }
) {
    val matcherState by rememberUpdatedState(matcher)
    val onDragState by rememberUpdatedState(onDrag)
    Modifier.pointerInput(*keys) {
        detectDragGesturesAbsolute(matcherState, initialOffset, onDragEnd, onDrag)
    }
}
