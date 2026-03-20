package dev.fishies.ranim2.gui

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.fishies.ranim2.Animation
import dev.fishies.ranim2.gui.util.toComposeColors
import dev.fishies.ranim2.theming.LocalTheme
import dev.fishies.ranim2.theming.defaultTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    animations: Outcome<List<AnimationData>>,
    paused: Boolean,
    setPaused: (Boolean) -> Unit,
    activeAnimation: Animation?,
    setActiveAnimation: (AnimationData) -> Unit = {},
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
                        // AnimationPicker(
                        // )
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
            AnimationPicker(animations, setActiveAnimation = setActiveAnimation, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
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
    // TextField(
    //     value = selectedOption?.name ?: "None",
    //     onValueChange = {},
    //     readOnly = true,
    //     trailingIcon = {
    //         ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
    //     },
    //     colors = ExposedDropdownMenuDefaults.textFieldColors(),
    //     modifier = Modifier.fillMaxWidth(),
    //     enabled = !disabled,
    // )
    //
    // DropdownMenu(
    //     expanded,
    //     onDismissRequest = { expanded = false },
    //     modifier = Modifier.exposedDropdownSize(),
    // ) {
    //     if (animations is Outcome.Success) {
    //         for (animation in animations.data) {
    //             DropdownMenuItem(onClick = { selectedOption = animation }) {
    //                 Text(animation.name)
    //             }
    //         }
    //     }
    // }
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

@Preview
@Composable
internal fun MainScreenPreview() {
    MaterialTheme(colors = defaultTheme.toComposeColors()) {
        MainScreen(Outcome.Success(emptyList()), false, {}, null, {})
    }
}
