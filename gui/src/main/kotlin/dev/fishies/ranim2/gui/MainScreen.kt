package dev.fishies.ranim2.gui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
import dev.fishies.ranim2.Container
import dev.fishies.ranim2.gui.util.toComposeColors
import dev.fishies.ranim2.theming.LocalTheme
import dev.fishies.ranim2.theming.defaultTheme
import dev.fishies.ranim2.util.saveImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MainScreen(
    animations: Outcome<List<AnimationData>>,
    paused: Boolean,
    setPaused: (Boolean) -> Unit,
    activeAnimation: Animation?,
    setActiveAnimation: (Animation?) -> Unit
) {
    LaunchedEffect(paused) {
        if (!paused && activeAnimation != null) {
            while (!activeAnimation.isFinished) {
                withFrameMillis {
                    activeAnimation.tick()
                }
            }
        }
    }
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    val graphicsLayer = rememberGraphicsLayer().configureAnimation(activeAnimation, layerSize)

    Box(Modifier.background(MaterialTheme.colors.background).fillMaxSize().onSizeChanged { layerSize = it }) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
            Column {
                LayerDisplay(graphicsLayer, Modifier.fillMaxWidth().weight(1f)) { layerSize = it }
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button({ setPaused(!paused) }) {
                        Text(if (paused) "Play" else "Pause")
                    }

                    Button({ runBlocking { graphicsLayer.saveImage() } }) {
                        Text("Save image")
                    }

                    Row {
                        Text("Debug layout bounds", Modifier.align(Alignment.CenterVertically))
                        with(Container) {
                            Switch(drawContainerOutlines, { drawContainerOutlines = it })
                        }
                    }

                    var expanded by remember { mutableStateOf(false) }
                    var selectedOption: AnimationData? by remember { mutableStateOf(null) }
                    LaunchedEffect(selectedOption) {
                        selectedOption?.let {
                            setActiveAnimation(it.fn())
                        }
                    }
                    ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }) {
                        TextField(
                            value = selectedOption?.name ?: "None",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.exposedDropdownSize()
                        ) {
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
            }
        }
    }
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
    Canvas(Modifier.fillMaxSize()) {
        drawBorderGradient(color.value, width = borderWidth.value)
    }
}

private fun DrawScope.drawBorderGradient(color: Color, width: Float = 15.0f) {
    val transparent = color.copy(alpha = 0.0f)
    val hStops = arrayOf(
        0.0f to color,
        width / size.width to transparent,
        1.0f - (width / size.width) to transparent,
        1.0f to color
    )
    val vStops = arrayOf(
        0.0f to color,
        width / size.height to transparent,
        1.0f - (width / size.height) to transparent,
        1.0f to color
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
