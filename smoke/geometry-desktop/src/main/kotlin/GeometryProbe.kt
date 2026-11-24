import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.*
import com.mad.maduikit.*
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

private fun tokens() {
    val corners = MadCornerRadii(0, 6, 10)
    check(MadCornerRole.entries.map { corners[it] } == listOf(0, 6, 10))
    val borders = MadBorderWidths(2, 3)
    check(MadBorderRole.entries.map { borders[it] } == listOf(2, 3))
    check(MadCornerRadii(0, 0, 0)[MadCornerRole.Small] == 0)
    check(MadBorderWidths(2, 2)[MadBorderRole.Strong] == 2)
    check(MadCornerRole.entries.map { MadGeometryDefaults.CornerRadii[it] } == listOf(4, 8, 16))
    check(MadBorderRole.entries.map { MadGeometryDefaults.BorderWidths[it] } == listOf(1, 2))
    listOf<() -> Any>({ MadCornerRadii(-1, 1, 2) }, { corners.copy(mediumDp = 11) },
        { corners.copy(smallDp = 7) }, { MadBorderWidths(0, 1) },
        { MadBorderWidths(-1, 2) }, { borders.copy(strongDp = 1) }).forEach {
        check(runCatching(it).exceptionOrNull() is IllegalArgumentException)
    }
}

@OptIn(InternalComposeUiApi::class)
private fun pixels(d: Int) {
    val scene = CanvasLayersComposeScene(density = Density(d.toFloat()), size = IntSize(96*d, 96*d))
    val surface = Surface.makeRasterN32Premul(96*d, 96*d)
    val dividerWidth = mutableStateOf(64)
    val red = 0xffff0000.toInt()
    val green = 0xff00ff00.toInt()
    val blue = 0xff0000ff.toInt()
    val white = 0xffffffff.toInt()
    scene.setContent {
        Box(Modifier.fillMaxSize().background(Color.White)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val c = MadCornerRadii(0, 6, 10)
                MadCornerRole.entries.forEach { role ->
                    Box(Modifier.size(24.dp).background(Color.Red, RoundedCornerShape(c[role].dp)))
                }
            }
            Column(Modifier.offset(y = 30.dp).width(dividerWidth.value.dp)) {
                MadDivider(color = Color.Green)
                MadDivider(role = MadBorderRole.Strong, color = Color.Blue)
                MadDivider(borderWidths = MadBorderWidths(2, 3), role = MadBorderRole.Strong, color = Color.Green)
                Box(Modifier.height(1.dp)) {
                    MadDivider(borderWidths = MadBorderWidths(2, 3), role = MadBorderRole.Strong, color = Color.Blue)
                }
            }
        }
    }
    fun measure(w: Int, t: Long) {
        repeat(3) { scene.render(surface.canvas.asComposeCanvas(), t + it*20_000_000) }
        val bitmap = Bitmap()
        try {
            bitmap.allocN32Pixels(96*d, 96*d)
            check(surface.readPixels(bitmap, 0, 0))
            fun p(x: Int, y: Int) = bitmap.getColor(x, y)
            check(p(0,0) == red && p(28*d,0) == white && p(56*d,0) == white)
            check(p(12*d,0) == red && p(40*d,0) == red && p(68*d,0) == red)
            fun inset(x: Int) = (0 until 12*d).first { p(x*d+it,0) == red }
            val insets = listOf(inset(0), inset(28), inset(56))
            check(insets[0] == 0 && insets[1] in 2*d..6*d && insets[2] in 5*d..10*d &&
                insets[2] > insets[1])
            val expected = listOf(green, blue, blue, green, green, green, blue)
            expected.forEachIndexed { offset, ink ->
                repeat(d) { row ->
                    val y = (30+offset)*d+row
                    check(p(0,y) == ink && p(w*d-1,y) == ink && p(w*d,y) == white)
                }
            }
            check((0 until w*d).all { p(it,37*d) == white })
            println("PASS density=${d}x width=${w*d}px border=${d}/${2*d}/${3*d}px capped=${d}px corner top insets=$insets")
        } finally { bitmap.close() }
    }
    try {
        measure(64, 0)
        dividerWidth.value = 31
        measure(31, 100_000_000)
    } finally { scene.close(); surface.close() }
}

fun main() {
    val result = runCatching {
        tokens()
        println("PASS role mapping, defaults, equal roles and six invalid constructor/copy cases")
        pixels(1)
        pixels(2)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if (result.isSuccess) 0 else 1)
}
