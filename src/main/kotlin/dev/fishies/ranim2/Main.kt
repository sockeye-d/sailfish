package dev.fishies.ranim2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.DrawerDefaults.shape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withCompositionLocal
import androidx.compose.ui.Alignment
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.SkiaGraphicsContext
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.fishies.ranim2.core.animation
import dev.fishies.ranim2.core.tween
import dev.fishies.ranim2.core.yield
import dev.fishies.ranim2.elements.makePainter
import dev.fishies.ranim2.elements.makeText
import dev.fishies.ranim2.ranim2.generated.resources.Res
import dev.fishies.ranim2.ranim2.generated.resources.skull_list
import dev.fishies.ranim2.tweener.In
import dev.fishies.ranim2.tweener.Out
import dev.fishies.ranim2.tweener.quadratic
import dev.fishies.ranim2.tweener.quartic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import javax.imageio.ImageIO
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

val anim = animation {
    //val shape = makeRectangle(
    //    size = Size(10f, 10f),
    //    color = Color.Black,
    //    position = Offset(20f, 100f),
    //    radius = 8.0f,
    //    style = Stroke(width = 2.0f)
    //)

    val resource = Res.drawable.skull_list
    //val painter = loadSvg("drawable/skull_list.svg")
    val painter = loadImage("drawable/cat_thumbsup.webp")
    //val shape = makePainter(
    //    painter,
    //    //tint = Color.Red,
    //    rotation = 40f,
    //    //color = Color.Black,
    //    //position = Offset(20f, 100f),
    //    //radius = 8.0f,
    //    //style = Stroke(width = 2.0f)
    //)
    val shape = makeText("This is some text", FontFamily.Monospace)
    //shape.size *= 0.05f
    val length = 120
    while (true) {
        yield(
            shape::position.tween(to = Offset(20f, 40f), length = length, tweener = quadratic(Out)),
            //shape::size.tween(to = Size(10f, 10f), length = length, tweener = quartic(Out)),
            shape::rotation.tween(to = shape.rotation + 140f, length = length, tweener = quadratic(Out)),
        )
        yield(
            shape::position.tween(to = Offset(20f, 100f), length = length, tweener = quadratic(In)),
            //shape::size.tween(to = Size(8f, 14f), length = length, tweener = quartic(In)),
            shape::rotation.tween(to = shape.rotation + 40f, length = length, tweener = quartic(In)),
        )
    }
    //yield(circle1::rotation.tween(to = 360.0f, length = 50))
    //circle1.rotation = 0.0f
    //yield(circle1::color.tween(to = Color.Red, length = 50, tweener = expoTweener(3.0, Out)))
    //while (true) {
    //    yield(circle1::position.tween(to = DpOffset(100.dp, 0.dp), length = 50, tweener = expoTweener(3.0, InOut)))
    //    yield(
    //        circle1::position.tween(to = DpOffset(0.dp, 0.dp), length = 50, tweener = expoTweener(3.0, Out)),
    //        circle1::rotation.tween(to = circle1.rotation + 90.0f, length = 50, tweener = expoTweener(3.0, Out))
    //    )
    //}
}

private fun loadSvg(path: String): Painter =
    loadSvgPainter(runBlocking { Res.readBytes(path) }.inputStream(), Density(1.0f))

private fun loadImage(path: String, filterQuality: FilterQuality = FilterQuality.Low): Painter =
    //loadSvgPainter(runBlocking { Res.readBytes(path) }.inputStream(), Density(1.0f))
    BitmapPainter(loadImageBitmap(runBlocking { Res.readBytes(path) }.inputStream()), filterQuality = filterQuality)

@OptIn(InternalComposeUiApi::class)
fun main() = application {
    val layer = withCompositionLocal(LocalGraphicsContext provides SkiaGraphicsContext()) {
        val graphicsLayer = rememberGraphicsLayer()
        graphicsLayer.record(Density(3f), LayoutDirection.Ltr, IntSize(300, 300)) {
            with(anim) { draw() }
        }
        graphicsLayer
    }
    Window(onCloseRequest = ::exitApplication, title = "My Desktop App") {
        MaterialTheme {
            LaunchedEffect(Unit) {
                while (!anim.isFinished) {
                    anim.tick()
                    delay(3.57.milliseconds)
                }
            }

            Column {
                Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    drawLayer(layer)
                }
                Row(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Button({ runBlocking { layer.saveImage() } }) {
                        Text("Save image")
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
