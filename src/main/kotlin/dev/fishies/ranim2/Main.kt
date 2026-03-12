package dev.fishies.ranim2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withCompositionLocal
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SkiaGraphicsContext
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fishies.ranim2.containers.Anchor
import dev.fishies.ranim2.containers.anchor
import dev.fishies.ranim2.containers.box
import dev.fishies.ranim2.containers.linear
import dev.fishies.ranim2.containers.panel
import dev.fishies.ranim2.containers.respectsPadding
import dev.fishies.ranim2.core.*
import dev.fishies.ranim2.elements.painter
import dev.fishies.ranim2.elements.rectangle
import dev.fishies.ranim2.elements.text
import dev.fishies.ranim2.ranim2.generated.resources.Res
import dev.fishies.ranim2.theming.Theme
import dev.fishies.ranim2.theming.defaultTheme
import dev.fishies.ranim2.theming.toComposeColors
import dev.fishies.ranim2.tweener.InOut
import dev.fishies.ranim2.tweener.cubic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.reflect.jvm.reflect
import kotlin.time.Duration.Companion.milliseconds

//private fun svgPainter(resource: DrawableResource): Painter {
//    resource
//val resourceReader = LocalResourceReader.currentOrPreview
//val density = LocalDensity.current
//val svgPainter by rememberResourceState(resource, resourceReader, density, { emptySvgPainter }) { env ->
//    val path = resource.getResourceItemByEnvironment(env).path
//    val cached = loadImage(path, path, resourceReader) {
//        ImageCache.Svg(it.toSvgElement().toSvgPainter(density))
//    } as ImageCache.Svg
//    cached.painter
//}
//return svgPainter
//}

val catppuccinMocha = loadJson<Theme>(Res.getUri("files/catppuccin-mocha.json"))

fun bug() = animation {
    val circle = painter(loadImage(Res.getUri("drawable/bug.png")))
    circle.size *= 0.1f

    repeat(10) {
        yield(
            circle::position.tween(
                to = Offset(Random.nextFloat() * 30.0f + 10.0f, Random.nextFloat() * 30.0f + 10.0f),
                length = 50,
                tweener = cubic(InOut),
            )
        )
        yield(frames = 20)
    }
}

fun anim() = animation {
    Container.drawContainerOutlines = true
    val state = object {
        var colorPickerColor by mutableStateOf(Color.Red)
    }
    panel(radius = 8.0f) {
        padding = Padding(all = 8.0f)
        linear(Y) {
            separation = 10F
            rectangle(Size(100f, 100f), Color.Red)() {
                ::color.bind(this) { state.colorPickerColor.copy(red = 1.0f) }
            }
            box {
                padding = Padding(all = 4.0f)
                rectangle(Size(0.0f, 15.0f), Color.Red)() {
                    anchor = Anchor.fill
                    respectsPadding = false
                }
                text("R")() {
                    anchor = Anchor.Tall.right
                }
            }
        }
    }
//    while (true) {
//        state.colorPickerColor = Color.White
//        yield()
//        state.colorPickerColor = Color.Green
//        yield()
//        state.colorPickerColor = Color.Red
//        yield()
//    }
    yield(state::colorPickerColor.tween(to = Color.Green, length = 100))
//    yield(panel::size.tween(to = Size(100.0f, panel.size.height), length = 100, tweener = cubic(InOut)))
}

@OptIn(InternalComposeUiApi::class)
fun main() = application {
    var layerSize by remember { mutableStateOf(IntSize.Zero) }
    var anim: Animation? by remember { mutableStateOf(anim()) }
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
            LaunchedEffect(Unit) {
                while (!(anim?.isFinished ?: true)) {
                    withFrameMillis {
                        anim?.tick()
                    }
                }
                anim = null
                delay(1000.milliseconds)
                System.gc()
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

private suspend fun GraphicsLayer.saveImage(file: File = File("/home/fish/Downloads/file.png")) =
    withContext(Dispatchers.IO) {
        ImageIO.write(
            toImageBitmap().toAwtImage(),
            file.extension,
            file,
        )
    }
