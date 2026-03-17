import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fishies.ranim2.Animation
import dev.fishies.ranim2.CompositeElement
import dev.fishies.ranim2.Container
import dev.fishies.ranim2.Padding
import dev.fishies.ranim2.animation
import dev.fishies.ranim2.bind
import dev.fishies.ranim2.containers.*
import dev.fishies.ranim2.containers.Anchor
import dev.fishies.ranim2.elements.rectangle
import dev.fishies.ranim2.elements.text
import dev.fishies.ranim2.gui.generated.resources.Res
import dev.fishies.ranim2.theming.*
import dev.fishies.ranim2.treeString
import dev.fishies.ranim2.tween
import dev.fishies.ranim2.tweener.InOut
import dev.fishies.ranim2.tweener.quadratic
import dev.fishies.ranim2.util.*
import dev.fishies.ranim2.yield
import kotlinx.coroutines.runBlocking
import kotlin.math.cos
import kotlin.math.sin
import kotlin.reflect.KProperty1

val catppuccinMocha = loadJson<Theme>(Res.getUri("files/catppuccin-mocha.json"))

class ColorPickerState {
    var color by mutableStateOf(Color.Red)
    var boxSize by mutableStateOf(Size(100f, 100f))
}

fun CompositeElement.channelBar(
    state: ColorPickerState,
    label: String,
    color: Color,
    channel: KProperty1<Color, Float>,
) = linear(Axis.X) {
    separation = 10F
    box {
        fraction = 1.0f
        rectangle(Size(0.0f, 15.0f), color)() {
            anchor = Anchor.fill
            respectsPadding = false
        }
        rectangle(Size(2.0f, 0.0f), theme.onBackground)() {
            ::anchor.bind {
                val color = state.color.convert(ColorSpaces.LinearSrgb)
                Anchor(
                    AxisAnchor.Absolute(channel(color)),
                    AxisAnchor.Fill,
                )
            }
        }
    }
    text(label)
}

private fun CompositeElement.colorPicker(state: ColorPickerState) = panel(radius = 8.0f) {
    padding = Padding(all = 8.0f)
    linear(Axis.Y) {
        separation = 10F
        rectangle(Size(100f, 100f))() {
            ::customMinimumSize.bind { state.boxSize }
            ::color.bind { state.color }
        }

        channelBar(state, "R", Color.Red, Color::red)
        channelBar(state, "G", Color.Green, Color::green)
        channelBar(state, "B", Color.Blue, Color::blue)
    }
}

val anim = animation {
    theme = catppuccinMocha
    val state = ColorPickerState()

    val picker = colorPicker(state)
    picker.position = Offset(100f, 100f)

    yield(animation {
        while (true) {
            yield(
                state::color.tween(
                    to = theme.primary, length = 300, tweener = quadratic(InOut), colorSpace = ColorSpaces.Oklab
                )
            )
            yield(
                state::color.tween(
                    to = theme.secondary, length = 300, tweener = quadratic(InOut), colorSpace = ColorSpaces.Oklab
                )
            )
            yield(
                state::color.tween(
                    to = theme.error, length = 300, tweener = quadratic(InOut), colorSpace = ColorSpaces.Oklab
                )
            )
        }
    }, animation {
        while (false) {
            state.boxSize = Size(
                cos(ticks / 30f),
                sin(ticks / 30f),
            ) * 50f + Size(100f, 100f)
            yield()
        }
    })
}

@OptIn(InternalComposeUiApi::class)
fun main() = application {
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    var anim: Animation? by remember { mutableStateOf(anim.apply { tick() }) }
    val layer: GraphicsLayer = withCompositionLocal(LocalGraphicsContext provides SkiaGraphicsContext()) {
        val graphicsLayer = rememberGraphicsLayer()
        graphicsLayer.record(Density(3f), LayoutDirection.Ltr, layerSize) {
            anim?.run {
                runLayoutPass()
                draw()
            }
        }
        graphicsLayer
    }

    MaterialTheme(colors = defaultTheme.toComposeColors()) {
        Window(onCloseRequest = ::exitApplication, title = "My Desktop App") {
            var paused by remember { mutableStateOf(true) }
            LaunchedEffect(paused) {
                if (!paused) {
                    while (!(anim?.isFinished ?: true)) {
                        withFrameMillis {
                            anim?.tick()
                        }
                    }
                }
            }

            Box(Modifier.background(MaterialTheme.colors.background).fillMaxSize().onSizeChanged { layerSize = it }) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onBackground) {
                    Column {
                        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            drawLayer(layer)
                        }
                        Row(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button({ paused = !paused }) {
                                Text(if (paused) "Play" else "Pause")
                            }

                            Button({ runBlocking { layer.saveImage() } }) {
                                Text("Save image")
                            }

                            Button({ println(anim?.treeString()) }) {
                                Text("Dump scene tree")
                            }

                            Row {
                                Text("Debug layout bounds", Modifier.align(Alignment.CenterVertically))
                                with(Container) {
                                    Switch(drawContainerOutlines, { drawContainerOutlines = it })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Theme.toComposeColors() = Colors(
    primary = primary,
    primaryVariant = primaryVariant,
    secondary = secondary,
    secondaryVariant = secondaryVariant,
    background = background,
    surface = surface,
    error = error,
    onPrimary = onPrimary,
    onSecondary = onSecondary,
    onBackground = onBackground,
    onSurface = onSurface,
    onError = onError,
    isLight = isLight
)
