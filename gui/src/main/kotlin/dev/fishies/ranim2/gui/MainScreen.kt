package dev.fishies.ranim2.gui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.*
import dev.fishies.ranim2.Animation
import dev.fishies.ranim2.gui.util.onDragAbsolute
import dev.fishies.ranim2.theming.LocalTheme
import dev.fishies.ranim2.util.exp10
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    animations: Outcome<List<AnimationData>>,
    paused: Boolean,
    setPaused: (Boolean) -> Unit,
    activeAnimation: Animation?,
    setActiveAnimation: (AnimationData) -> Unit = {},
    cursorFrameState: MutableState<Int>,
    setCursorFrame: (Int) -> Unit = {},
) {
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val graphicsLayer = rememberGraphicsLayer().configureAnimation(activeAnimation, layerSize)

    Box(Modifier.background(MaterialTheme.colors.background).fillMaxSize().onSizeChanged { layerSize = it }) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
            Column {
                LayerDisplay(graphicsLayer, Modifier.fillMaxWidth().weight(1f)) { layerSize = it }
                Surface(Modifier.fillMaxWidth()) {
                    Column {
                        PlayControlBar(
                            paused,
                            setPaused,
                            {},
                            {},
                            animations,
                            setActiveAnimation = setActiveAnimation,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )

                        ScrubBar(remember {
                            ScrubBarState(
                                cursorFrameState = cursorFrameState,
                            )
                        }, modifier = Modifier.height(64.dp).fillMaxWidth())
                    }
                }
            }
        }
    }
    ReloadHighlight(animations, modifier = Modifier.fillMaxSize())
}

@Composable
private fun PlayControlBar(
    paused: Boolean,
    setPaused: (Boolean) -> Unit,
    skipBackwards: () -> Unit,
    skipForwards: () -> Unit,
    animations: Outcome<List<AnimationData>>,
    setActiveAnimation: (AnimationData) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier) {
        Box(Modifier.weight(1f))
        Row {
            IconButton(skipBackwards) {
                Icon(
                    Icons.Rounded.SkipPrevious, null
                )
            }
            IconToggleButton(paused, { setPaused(!paused) }) {
                Icon(
                    if (paused) Icons.Rounded.PlayCircle else Icons.Rounded.PauseCircle, null, Modifier.size(32.dp)
                )
            }
            IconButton(skipForwards) {
                Icon(
                    Icons.Rounded.SkipNext, null
                )
            }
        }
        Box(Modifier.weight(1f)) {
            AnimationPicker(
                animations,
                setActiveAnimation = setActiveAnimation,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun AnimationPicker(
    animations: Outcome<List<AnimationData>>,
    modifier: Modifier = Modifier,
    setActiveAnimation: (AnimationData) -> Unit = {},
) {
    val disabled = (animations.dataOrNull?.size ?: 0) == 0
    var expanded by remember { mutableStateOf(false) }
    var selectedOption: AnimationData? by remember { mutableStateOf(null) }
    LaunchedEffect(selectedOption) {
        selectedOption?.let { setActiveAnimation(it) }
    }
    Chip({ if (!disabled) expanded = true }, modifier = modifier, enabled = !disabled) {
        Text(selectedOption?.name ?: "None")
        DropdownMenu(expanded, onDismissRequest = { expanded = false }) {
            if (animations is Outcome.Success) {
                for (animation in animations.data) {
                    DropdownMenuItem(onClick = { selectedOption = animation }) {
                        Text(animation.name)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReloadHighlight(animations: Outcome<*>, modifier: Modifier = Modifier) {
    val color = remember { Animatable(Color.Transparent) }
    val borderWidth = remember { Animatable(0.0f) }
    val theme = LocalTheme.current
    LaunchedEffect(animations) {
        if (animations == Outcome.Progress) {
            launch {
                borderWidth.animateTo(10.0f, tween(300, easing = LinearOutSlowInEasing))
            }
            color.animateTo(theme.warning.copy(alpha = 0.5f), tween(300, easing = LinearOutSlowInEasing))
        } else if (animations is Outcome.Success) {
            launch {
                borderWidth.animateTo(25.0f, tween(300, easing = LinearOutSlowInEasing))
                borderWidth.animateTo(0.0f, tween(300, easing = FastOutLinearInEasing))
            }
            color.animateTo(theme.success, tween(300, easing = LinearOutSlowInEasing))
            color.animateTo(Color.Transparent, tween(300, easing = FastOutLinearInEasing))
        }
    }
    Canvas(modifier) {
        drawBorderGradient(color.value, width = borderWidth.value)
    }
}

private fun DrawScope.drawBorderGradient(color: Color, width: Float = 15.0f) {
    val transparent = color.copy(alpha = 0.0f)
    val hStops = arrayOf(
        0.0f to color, width / size.width to transparent, 1.0f - (width / size.width) to transparent, 1.0f to color
    )
    val vStops = arrayOf(
        0.0f to color, width / size.height to transparent, 1.0f - (width / size.height) to transparent, 1.0f to color
    )
    val hBrush = Brush.horizontalGradient(*hStops)
    val vBrush = Brush.verticalGradient(*vStops)
    drawRect(hBrush)
    drawRect(vBrush)
}

@Stable
class ScrubBarState(cursorFrameState: MutableState<Int>) {
    var scroll by mutableFloatStateOf(0f)
    var zoom by mutableFloatStateOf(1f)
    var cursorFrame by cursorFrameState

    val speed = 2.0f
}

private val tickShape = RoundedCornerShape(2.0.dp)
private val knobShape = GenericShape { (width, height), _ ->
    lineTo(width, 0.0f)
    lineTo(width * 0.5f, height)
    close()
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ScrubBar(state: ScrubBarState, modifier: Modifier = Modifier) {
    val contentColor = LocalContentColor.current
    val tickColor = lerp(LocalContentColor.current, MaterialTheme.colors.surface, 0.5f)
    val primaryColor = MaterialTheme.colors.primary
    val measurer = rememberTextMeasurer(1000)
    var mouseX by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }

    val smoothed by animateOffsetAsState(Offset(state.scroll, state.zoom), spring(stiffness = 800f))
    val localFontSize = LocalTextStyle.current.fontSize

    fun updateCursorFrame() {
        state.cursorFrame = ((mouseX + state.scroll) / 50f / state.zoom).roundToInt()
    }

    val modifier = modifier.scrollable(rememberScrollableState {
        state.scroll = (state.scroll - it * state.speed).coerceAtLeast(-100f)
        it
    }, Orientation.Vertical).transformable(rememberTransformableState { zoomChange, _, _ ->
        val zoomChange = zoomChange.pow(state.speed)
        state.zoom *= zoomChange
        state.scroll = (-mouseX + (mouseX + state.scroll) * zoomChange).coerceAtLeast(-100f)
    }).pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val e = awaitPointerEvent()
                if (e.type == PointerEventType.Move) {
                    mouseX = e.changes.first().position.x
                    if (pressed) {
                        updateCursorFrame()
                    }
                }
                if (e.button == PointerButton.Primary && e.type == PointerEventType.Press) {
                    pressed = true
                    updateCursorFrame()
                }
                if (e.button == PointerButton.Primary && e.type == PointerEventType.Release) {
                    pressed = false
                }
            }
        }
    }
    val textSize by derivedStateOf { measurer.measure("0", TextStyle(fontSize = localFontSize)).multiParagraph.height }
    val scroll by derivedStateOf { smoothed.x }
    val zoom by derivedStateOf { smoothed.y }
    Canvas(modifier) {
        val scale = log10(3f / zoom)
        val scaleFloor = scale.toInt().coerceAtLeast(0)
        val powerScaleFloor = exp10(scaleFloor)
        val powerScaleCeil = exp10(scaleFloor + 1)
        val scaleFrac = ((3f / zoom) - powerScaleFloor) / (powerScaleFloor * 9)
        val progression = SnappedIntProgression(
            // A little padding to make sure numbers don't get cut off at the ends
            (scroll / 50.0f / zoom).toInt() - powerScaleFloor,
            ((scroll + size.width) / 50.0f / zoom).toInt() + 1 + powerScaleFloor,
            powerScaleFloor
        )
        for (frame in progression) {
            val alpha = if (frame % powerScaleCeil == 0) 1.0f else (1.0f - scaleFrac).pow(2.0f)
            val color = contentColor.copy(alpha = alpha)

            val x = frame * 50f * zoom - scroll
            val tickSize = Size(2.0f, (size.height - textSize) * alpha)
            withTransform({ translate(left = x - 1.0f, top = textSize) }) {
                drawOutline(
                    tickShape.createOutline(tickSize, layoutDirection, Density(density)), tickColor.copy(alpha = alpha)
                )
            }
            val result = measurer.measure("$frame", TextStyle(fontSize = localFontSize * (alpha * 0.5 + 0.5)))
            drawText(
                result, color, Offset(x - result.multiParagraph.width * 0.5f, 20.0f - result.multiParagraph.height)
            )
        }
        val cursorX = state.cursorFrame * 50f * zoom - scroll
        withTransform({ translate(left = cursorX - 1.0f, top = textSize) }) {
            drawOutline(
                tickShape.createOutline(Size(2.0f, size.height - textSize), layoutDirection, Density(density)),
                primaryColor
            )
        }
        withTransform({ translate(left = cursorX - 4.0f, top = textSize) }) {
            drawOutline(
                knobShape.createOutline(Size(8.0f, 8.0f * sqrt(3.0f) / 2.0f), layoutDirection, Density(density)),
                primaryColor
            )
        }
    }
    var frame by remember { mutableIntStateOf(0) }
    Box(
        Modifier.absoluteOffset(x = (frame * 50f * zoom - scroll).dp)
            .size(10.dp)
            .background(Color.Red)
            .onDragAbsolute(initialOffset = { Offset(frame * 50f * zoom - scroll, 0f) }) { (x, _) ->
                println(zoom)
                frame = ((x + scroll) / 50f / zoom).roundToInt()
            },
    )
}

class SnappedIntProgression(val min: Int, val max: Int, val step: Int) : Iterable<Int> {
    override fun iterator(): IntIterator = Iterator(min, max, step)

    private class Iterator(min: Int, val max: Int, val step: Int, sign: Int = if (min < 0) 0 else 1) : IntIterator() {
        var current: Int = if (min / step * step == min) min else (min / step + sign) * step
        override fun nextInt(): Int {
            val last = current
            current += step
            return last
        }

        override fun hasNext() = current <= max
    }
}
