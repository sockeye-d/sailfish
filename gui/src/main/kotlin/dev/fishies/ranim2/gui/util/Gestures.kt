package dev.fishies.ranim2.gui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput

@OptIn(ExperimentalFoundationApi::class)
suspend fun PointerInputScope.detectDragGesturesAbsolute(
    matcher: PointerMatcher = PointerMatcher.Primary, initialOffset: () -> Offset, onDrag: (Offset) -> Unit
) {
    var absoluteDragPosition = Offset.Unspecified
    detectDragGestures(matcher, onDragStart = { absoluteDragPosition = it + initialOffset() }) {
        absoluteDragPosition += it
        onDrag(absoluteDragPosition)
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.onDragAbsolute(matcher: PointerMatcher = PointerMatcher.Primary, initialOffset: () -> Offset, onDrag: (Offset) -> Unit) = composed(
    inspectorInfo = {
        name = "onDragAbsolute"
        properties["matcher"] = matcher
        properties["onDrag"] = onDrag
    }
) {
    val matcherState by rememberUpdatedState(matcher)
    val onDragState by rememberUpdatedState(onDrag)
    Modifier.pointerInput(Unit) {
        detectDragGesturesAbsolute(matcherState, initialOffset, onDragState)
    }
}
