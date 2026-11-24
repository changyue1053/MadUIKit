import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import androidx.compose.material3.MaterialTheme
import com.mad.maduikit.*
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun probe(dark: Boolean, rtl: Boolean, scale: Float, mode: String?) {
    val owners = mutableSetOf<SemanticsOwner>()
    val host = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene = CanvasLayersComposeScene(density = Density(1f, scale), size = IntSize(260,280), platformContext = host)
    val surface = Surface.makeRasterN32Premul(260,280)
    var statusSize = IntSize.Zero
    var countSize = IntSize.Zero
    var statusBox = Color.Transparent
    var statusInk = Color.Transparent
    val count = if (rtl) "١٢" else "12"
    val phrase = if (rtl) "١٢ رسالة غير مقروءة" else "12 unread messages"
    val label = if (rtl) "الحالة متاحة الآن للقراءة بدون اتصال" else "Available now for offline reading"
    scene.setContent {
        MadTheme(darkTheme = dark) {
            statusBox = MaterialTheme.colorScheme.secondaryContainer
            statusInk = MaterialTheme.colorScheme.onSecondaryContainer
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Column(Modifier.fillMaxSize().background(Color.White)) {
                    MadBadge(text = count, spokenLabel = if (mode == "blank-spoken") " " else phrase,
                        modifier = Modifier.onSizeChanged { countSize = it })
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.width(116.dp)) {
                        MadBadge(text = if (mode == "blank-text") " " else label,
                            modifier = Modifier.onSizeChanged { statusSize = it })
                    }
                }
            }
        }
    }
    try {
        repeat(3) { surface.canvas.clear(0); scene.render(surface.canvas.asComposeCanvas(), it*20_000_000L) }
        val merged = owners.single().getAllSemanticsNodes(mergingEnabled = true)
        val raw = owners.single().getAllSemanticsNodes(mergingEnabled = false)
        fun texts(n: SemanticsNode) = n.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }
        val countBadge = merged.single { it.config.getOrNull(SemanticsProperties.ContentDescription) == listOf(phrase) }
        val status = merged.single { texts(it) == listOf(label) }
        check(merged.flatMap(::texts) == listOf(label)) { "visible count duplicated in exposed semantics" }
        check(merged.flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() } == listOf(phrase))
        check((raw + merged).none { it.config.contains(SemanticsActions.OnClick) ||
            it.config.contains(SemanticsProperties.LiveRegion) ||
            it.config.contains(SemanticsProperties.Role) })
        val getLayout = status.config[SemanticsActions.GetTextLayoutResult].action!!
        val result = mutableListOf<TextLayoutResult>()
        check(getLayout(result))
        val layout = result.single()
        check(layout.lineCount > 1 && !layout.hasVisualOverflow)
        check(layout.getParagraphDirection(0) == if (rtl) androidx.compose.ui.text.style.ResolvedTextDirection.Rtl
            else androidx.compose.ui.text.style.ResolvedTextDirection.Ltr)
        check(statusSize.width <= 116 && statusSize.height >= layout.size.height + 4)
        check(countSize.width >= 20 && countSize.height >= 20)
        val contrast = (maxOf(statusBox.luminance(), statusInk.luminance()) + .05f) /
            (minOf(statusBox.luminance(), statusInk.luminance()) + .05f)
        check(contrast >= 4.5f)
        val bounds = countBadge.boundsInRoot
        check(if (rtl) bounds.right == 260f else bounds.left == 0f)
        Bitmap().use { b ->
            b.allocN32Pixels(260,280); check(surface.readPixels(b,0,0))
            val x = bounds.left.toInt(); val right = bounds.right.toInt(); val y = bounds.top.toInt()
            check(b.getColor(x,y) == -1 && b.getColor(right-1,y) == -1) // rounded corners
            check(b.getColor(x+4,y+countSize.height/2) == statusBox.toArgb()) // flat pill interior
            check((y until y+countSize.height).any { yy ->
                (x+8 until right-8).any { b.getColor(it,yy) == statusInk.toArgb() }
            }) { "text color absent from actual raster" }
        }
        println("PASS dark=$dark rtl=$rtl scale=$scale  count=$countSize status=$statusSize lines=${layout.lineCount} contrast=${"%.2f".format(java.util.Locale.ROOT,contrast)}")
    } finally { scene.close(); surface.close() }
}

fun main(args: Array<String>) {
    val mode = args.singleOrNull()
    val result = runCatching {
        check(mode in setOf(null, "blank-spoken", "blank-text"))
        probe(false,false,1f,mode)
        if (mode == null) {
            probe(true,false,1f,null)
            probe(false,false,1.6f,null)
            probe(false,true,1f,null)
            probe(false,true,1.6f,null)
            probe(true,true,1.6f,null)
            auditBadgeShape()
        }
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
