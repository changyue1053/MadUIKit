import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.mad.maduikit.MadFilterTabs
import com.mad.maduikit.MadFilterTab
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val selected = mutableStateOf(0)
    val calls = mutableListOf<Int>()
    var hostAccepts = false
    val labels = listOf("All books", "Favorites", "Unavailable")
    val scene = ImageComposeScene(width = 320, height = 64, density = Density(1f, 1f)) {
        MadFilterTabs(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, label ->
                MadFilterTab(
                    selected = selected.value == i,
                    enabled = i != 2,
                    onSelect = {
                        calls += i
                        if (hostAccepts) selected.value = i
                    },
                    modifier = Modifier.size(100.dp, 56.dp).background(
                        if (selected.value == i) Color(0xFF314C81) else Color(0xFFE3E9F4)
                    ),
                ) {
                    BasicText(label, Modifier.align(Alignment.Center), style = TextStyle(
                        color = if (selected.value == i) Color.White else Color(0xFF23304B)
                    ))
                }
            }
        }
    }
    var now = 16_000_000L
    fun paint() = scene.render(now.also { now += 16_000_000L })
    fun tap(x: Float) {
        val at = Offset(x, 26f)
        scene.sendPointerEvent(PointerEventType.Press, at, timeMillis = now / 1_000_000, type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release, at, timeMillis = now / 1_000_000 + 1, type = PointerType.Touch)
        paint().close()
    }
    try {
        paint().close()
        paint().close()
        tap(155f)
        check(calls == listOf(1) && selected.value == 0) { "A request is not internal selection" }
        tap(260f)
        check(calls == listOf(1) && selected.value == 0) { "Disabled tab should not emit requests" }
        hostAccepts = true
        tap(155f)
        check(calls == listOf(1, 1) && selected.value == 1) { "Host controls selection" }
        if (args.isNotEmpty()) paint().use { bitmap ->
            checkNotNull(bitmap.encodeToData(EncodedImageFormat.PNG)).use { File(args[0]).writeBytes(it.bytes) }
        }
        println("Synthetic MadFilterTabs pointer smoke: controlled selection and disabled behavior verified.")
    } finally {
        scene.close()
    }
    exitProcess(0)
}
