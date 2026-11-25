import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadAvatar
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

private val left = Color(0xffffaa00)
private val right = Color(0xff1579e0)
private val edge = Color(0xfffb004d)
private val widePainter = object : Painter() {
    override val intrinsicSize = Size(80f,40f)
    override fun DrawScope.onDraw() {
        drawRect(edge)
        drawRect(left, topLeft = Offset(size.width/4,0f), size = Size(size.width/4,size.height))
        drawRect(right, topLeft = Offset(size.width/2,0f), size = Size(size.width/4,size.height))
    }
}

@OptIn(InternalComposeUiApi::class)
private fun probe(rtl: Boolean, scale: Float, mode: String?) {
    val dark = rtl
    val owners = mutableSetOf<SemanticsOwner>()
    val host = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene = CanvasLayersComposeScene(density = Density(1f,scale), size = IntSize(180,300), platformContext = host)
    val surface = Surface.makeRasterN32Premul(180,300)
    val background = Color(0xffdf0088)
    val theme = if (dark) darkColorScheme() else lightColorScheme()
    val circle = theme.primaryContainer
    val ink = theme.onPrimaryContainer
    val imageName = if (rtl) "صورة ليلى" else "Lina’s photo"
    val fallbackName = if (rtl) "حساب ليلى" else "Lina’s profile"
    val initials = if (rtl) "ل" else "AB"
    scene.setContent {
        MaterialTheme(colorScheme = theme) {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize().background(background)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MadAvatar(if (mode == "blank-name") " " else imageName, Modifier.size(64.dp,30.dp),
                            painter=widePainter, initials=initials)
                        MadAvatar(null, painter=widePainter)
                        MadAvatar(fallbackName, initials=if(mode=="blank-initials") " " else initials)
                        MadAvatar(null, initials=if(mode=="line-break") "A\nB" else initials)
                        MadAvatar(null, initials=if(rtl) "كلمة طويلة جدا جدا" else "EXTREMELY LONG INITIALS")
                        MadAvatar(null)
                        MadAvatar(null, Modifier.size(12.dp,18.dp), initials=initials)
                    }
                }
            }
        }
    }
    try {
        repeat(4) { surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),it*20_000_000L) }
        val owner = owners.single()
        for (merged in listOf(false,true)) {
            val nodes = owner.getAllSemanticsNodes(mergingEnabled=merged)
            val described = nodes.filter { it.config.contains(SemanticsProperties.ContentDescription) }
            check(described.flatMap { it.config[SemanticsProperties.ContentDescription] }.sorted() == listOf(imageName,fallbackName).sorted())
            check(described.all { it.config[SemanticsProperties.Role] == Role.Image })
            check(nodes.count { it.config.contains(SemanticsProperties.Role) } == 2)
            check(nodes.none { it.config.contains(SemanticsActions.OnClick) || it.config.contains(SemanticsProperties.Text) }) {
                "fallback or decorative avatar leaked semantics"
            }
            val painted = described.single { it.config[SemanticsProperties.ContentDescription] == listOf(imageName) }.boundsInRoot
            check(painted.width == 30f && painted.height == 30f)
        }
        val contrast = (maxOf(circle.luminance(),ink.luminance())+.05f)/(minOf(circle.luminance(),ink.luminance())+.05f)
        check(contrast >= 4.5f)
        Bitmap().use { b ->
            b.allocN32Pixels(180,300);check(surface.readPixels(b,0,0))
            fun expect(x:Int,y:Int,c:Color) { check(b.getColor(x,y)==c.toArgb()) { "pixel at $x,$y differs" } }
            fun x(w: Int)= if (rtl) 180-12-w else 12
            val wideX = x(64)+17
            expect(wideX,12,background)
            expect(wideX-1,27,background)
            expect(wideX+30,27,background)
            expect(wideX+5,27,left)
            expect(wideX+24,27,right)
            check((wideX until wideX+30).none { b.getColor(it,27)==edge.toArgb() })
            expect(x(40),48,background)
            expect(x(40)+30,68,right)
            fun pixels(y: Int, w: Int = 40, h: Int = 40) =
                (0 until h).flatMap { yy -> (0 until w).map { xx -> b.getColor(x(w)+xx,y+yy) } }
            val meaningful = pixels(94)
            val decorative = pixels(140)
            val overlong = pixels(186)
            val dot = pixels(232)
            check(dot == overlong && meaningful == decorative)
            check(meaningful != dot) { "initials did not render at this scale" }
            check(meaningful.count { it==ink.toArgb() } > 6)
            check(dot.count { it==ink.toArgb() } > 6)
            expect(x(12),278,background) // Vertically centered in 12x18; image cannot become ellipse.
            check(b.getColor(x(12)+6,287) !in setOf(background.toArgb(),circle.toArgb()))
        }
        println("PASS avatar RTL=$rtl dark=$dark scale=$scale: crop + circle, one name each, initials vs fallback, long/tight dot, contrast=${"%.2f".format(java.util.Locale.ROOT,contrast)}")
    } finally { scene.close(); surface.close() }
}
fun main(args: Array<String>) {
    val mode = args.singleOrNull()
    val result = runCatching {
        require(mode in setOf(null,"blank-name","blank-initials","line-break"))
        probe(false,1f,mode)
        if (mode==null) probe(true,1.6f,null)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
