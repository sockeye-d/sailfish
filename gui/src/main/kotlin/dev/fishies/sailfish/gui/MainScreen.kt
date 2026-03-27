package dev.fishies.sailfish.gui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.fishies.sailfish.Marker
import dev.fishies.sailfish.theming.LocalTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    animations: Outcome<List<AnimationData>>,
    state: CurrentAnimationState?,
    cursorFrame: Int,
    setCursorFrame: (Int) -> Unit,
    setPaused: (Boolean) -> Unit,
    setActiveAnimation: (AnimationData) -> Unit = {},
    modifyMarker: (String, Marker) -> Unit,
) {
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val graphicsLayer = rememberGraphicsLayer().configureAnimation(state?.animation, layerSize)

    Box(Modifier.background(MaterialTheme.colors.background).fillMaxSize().onSizeChanged { layerSize = it }) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
            Column {
                LayerDisplay(graphicsLayer, Modifier.fillMaxWidth().weight(1f)) { layerSize = it }
                Surface(Modifier.fillMaxWidth()) {
                    Column {
                        PlayControlBar(
                            state != null,
                            state?.paused ?: false,
                            setPaused,
                            {},
                            {},
                            animations,
                            setActiveAnimation = setActiveAnimation,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            left = {
                                if (state?.data != null) {
                                    Text("${state.data.symbol.data.framerate}")
                                }
                            },
                            right = {
                                AnimationPicker(
                                    animations, setActiveAnimation = setActiveAnimation, modifier = Modifier
                                )
                            })

                        val animationEnd by rememberUpdatedState(state?.animationLength)
                        val animationEndSmoothed by animateFloatAsState(
                            animationEnd?.toFloat() ?: 0.0f,
                            timelineElementAnimation
                        )
                        val secondary = MaterialTheme.colors.secondary
                        ScrubBar(
                            remember { ScrubBarState() },
                            cursorFrame,
                            setCursorFrame,
                            Modifier.height(128.dp).fillMaxWidth().padding(bottom = 8.dp),
                            drawContent = { getPosition, _ ->
                                animationEnd?.let { _ ->
                                    val startPos = getPosition(0.0f)
                                    val endPos = getPosition(animationEndSmoothed)
                                    drawRoundRect(
                                        secondary,
                                        Offset(startPos, 0.0f),
                                        Size(endPos - startPos, 20f),
                                        alpha = 0.2f,
                                        cornerRadius = CornerRadius(4f, 4f)
                                    )
                                    // drawRoundRect(secondary, Offset(startPos, 0.0f), Size(endPos - startPos, 10f), style = Stroke(2f), cornerRadius = CornerRadius(4f, 4f))
                                }
                            }
                        ) { getPosition, getFrame ->
                            for ((name, marker) in state?.markers ?: emptyMap()) {
                                val name by rememberUpdatedState(name)
                                val marker by rememberUpdatedState(marker)
                                TimelineHandle(
                                    getPosition,
                                    getFrame,
                                    marker.position,
                                    { modifyMarker(name, marker.copy(position = it)) },
                                    Modifier.align(Alignment.BottomStart)
                                ) {
                                    Row {
                                        Text(name)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    ReloadHighlight(animations, modifier = Modifier.fillMaxSize())
}

@Composable
private fun PlayControlBar(
    enabled: Boolean,
    paused: Boolean,
    setPaused: (Boolean) -> Unit,
    skipBackwards: () -> Unit,
    skipForwards: () -> Unit,
    animations: Outcome<List<AnimationData>>,
    setActiveAnimation: (AnimationData) -> Unit,
    modifier: Modifier = Modifier,
    left: @Composable RowScope.() -> Unit = {},
    right: @Composable RowScope.() -> Unit = {},
) {
    Row(modifier) {
        Row(
            Modifier.weight(1f).padding(start = 8.dp).align(Alignment.CenterVertically),
            Arrangement.Start,
            Alignment.CenterVertically
        ) {
            left()
        }
        Row {
            IconButton(skipBackwards, enabled = enabled) {
                Icon(Icons.Rounded.SkipPrevious, null)
            }
            IconToggleButton(paused, { setPaused(!paused) }, enabled = enabled) {
                Icon(if (paused) Icons.Rounded.PlayCircle else Icons.Rounded.PauseCircle, null, Modifier.size(32.dp))
            }
            IconButton(skipForwards, enabled = enabled) {
                Icon(Icons.Rounded.SkipNext, null)
            }
        }
        Row(
            Modifier.weight(1f).padding(end = 8.dp).align(Alignment.CenterVertically),
            Arrangement.End,
            Alignment.CenterVertically
        ) {
            right()
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
