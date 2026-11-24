import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadBadge
import com.mad.maduikit.MadTheme
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

private data class Input(val rtl: Boolean, val dark: Boolean, val width: Int, val scale: Float, val text: String)
private data class Raster(val width: Int, val height: Int, val fill: Int, val pixels: IntArray)
private const val W = 152
private const val H = 384
private val hostile = Color(0xffdf0078)

@OptIn(InternalComposeUiApi::class)
private fun render(case: Input, glyphFree: Boolean): Raster {
    val scene = CanvasLayersComposeScene(density = Density(1f, case.scale), size = IntSize(W,H))
    val surface = Surface.makeRasterN32Premul(W,H)
    var size = IntSize.Zero
    var fill = Color.Transparent
    scene.setContent {
        MadTheme(darkTheme = case.dark) {
            val actual = MaterialTheme.colorScheme
            fill = actual.secondaryContainer
            val palette = if (glyphFree) actual.copy(onSecondaryContainer = Color.Transparent) else actual
            MaterialTheme(colorScheme = palette) {
                CompositionLocalProvider(LocalLayoutDirection provides if (case.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    Box(Modifier.fillMaxSize().background(hostile)) {
                        Box(Modifier.width(case.width.dp)) {
                            MadBadge(case.text, Modifier.onSizeChanged { size = it })
                        }
                    }
                }
            }
        }
    }
    try {
        repeat(3) { surface.canvas.clear(0); scene.render(surface.canvas.asComposeCanvas(), it*20_000_000L) }
        Bitmap().use { b ->
            b.allocN32Pixels(W,H)
            check(surface.readPixels(b,0,0))
            return Raster(size.width, size.height, fill.toArgb(),
                IntArray(W*H) { i -> b.getColor(i%W,i/W) })
        }
    } finally { scene.close(); surface.close() }
}

fun auditBadgeShape(sensitivity: Boolean = false) {
    val cases = listOf(
        Input(false,false,116,1f,"Available now for offline reading"),
        Input(false,true,79,1.6f,"WWWW HHHH WWWW HHHH WWWW HHHH WWWW"),
        Input(false,false,74,1.8f,"FFFF FFFF FFFF FFFF FFFF FFFF FFFF"),
        Input(true,false,116,1.6f,"الحالة متاحة الآن للقراءة بدون اتصال"),
        Input(true,true,79,1.6f,"مكتمل مكتمل مكتمل مكتمل مكتمل مكتمل مكتمل"),
        Input(true,false,72,1.8f,"الحجز جاهز حاليا للنشر بالجملة الآن"),
    )
    var exposed = 0
    cases.forEach { case ->
        val actual = render(case,false)
        val mask = render(case,true)
        check(actual.width == mask.width && actual.height == mask.height && actual.fill == mask.fill)
        check(actual.width == case.width && actual.height in 41 until H)
        val glyph = actual.pixels.indices.filter { actual.pixels[it] != mask.pixels[it] }
        check(glyph.size > 100)
        val onHost = glyph.count { mask.pixels[it] == hostile.toArgb() }
        val onCurve = glyph.count { mask.pixels[it] != hostile.toArgb() && mask.pixels[it] != mask.fill }
        exposed += onHost + onCurve
        println("rtl=${case.rtl} dark=${case.dark} width=${actual.width} height=${actual.height} scale=${case.scale} glyph=${glyph.size} onHost=$onHost onCurve=$onCurve")
    }
    if (sensitivity) check(exposed > 0) { "baseline sensitivity failed" }
    else check(exposed == 0) { "glyph outside opaque badge fill: $exposed" }
    println("PASS exposures=$exposed reportOnly=$sensitivity")
}
