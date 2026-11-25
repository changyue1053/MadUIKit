import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.*
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun probe(rtl: Boolean, dark: Boolean, width: Int, mode: String?) {
    val owners = mutableSetOf<SemanticsOwner>()
    val host = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene = CanvasLayersComposeScene(density = Density(1f), size = IntSize(220,256), platformContext = host)
    val surface = Surface.makeRasterN32Premul(220,256)
    val outer = Color(0xff8800bb)
    val sentinel = Color(0xff1188ff)
    val spill = Color(0xffff7700)
    val base = Color(if (dark) 0xff202a32 else 0xffdef0f8)
    val ink = Color(if (dark) 0xfff4f1ec else 0xff172737)
    val stroke = Color(if (dark) 0xff88cca0 else 0xff1b7042)
    val theme = if (dark) darkColorScheme(surfaceContainer=base,onSurface=ink,outlineVariant=stroke)
        else lightColorScheme(surfaceContainer=base,onSurface=ink,outlineVariant=stroke)
    var count = 0
    val height = 34
    scene.setContent {
        MaterialTheme(colorScheme = theme) {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                LocalContentColor provides sentinel) {
                Box(Modifier.fillMaxSize().background(outer)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MadCard(Modifier.size(width.dp,height.dp)) {}
                        MadCard(Modifier.size(width.dp,height.dp), cornerRole = MadCornerRole.Small,
                            cornerRadii = MadCornerRadii(0,6,8), borderRole = MadBorderRole.Strong,
                            borderWidths = MadBorderWidths(2,3)) {}
                        MadCard(Modifier.size(width.dp,height.dp)) {
                            Box(Modifier.fillMaxWidth().height(4.dp).drawBehind {
                                drawRect(spill, topLeft=Offset(-4f,-4f),size=Size((width+8).toFloat(),(height+8).toFloat()))
                            })
                        }
                        MadCard(Modifier.size(width.dp,height.dp)) {
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.padding(horizontal=6.dp).size(8.dp).background(LocalContentColor.current))
                        }
                        Box(Modifier.size(6.dp).background(LocalContentColor.current))
                        MadCard(Modifier.size(width.dp,72.dp)) {
                            Text("Go", if (mode == "child-absent") Modifier else Modifier.height(48.dp).clickable(role=Role.Button) { count++ })
                        }
                    }
                }
            }
        }
    }
    var t = 0L
    fun frame() { repeat(4) { surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),t);t+=20_000_000 } }
    fun tap(p: Offset) {
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=t/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=t/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    try {
        frame()
        val start = if (rtl) 220-12-width else 12
        val end = start + width
        val child = owners.single().getAllSemanticsNodes(mergingEnabled=true).singleOrNull { it.config.contains(SemanticsActions.OnClick) } ?: error("expected sole caller-owned action")
        check(child.config[SemanticsProperties.Text].map { it.text } == listOf("Go"))
        check(child.config[SemanticsProperties.Role] == Role.Button)
        val y1=12
        val y2=y1+height+4
        val y3=y2+height+4
        val y4=y3+height+4
        Bitmap().use { b ->
            b.allocN32Pixels(220,256);check(surface.readPixels(b,0,0))
            fun expect(x:Int,y:Int,c:Color) { check(b.getColor(x,y)==c.toArgb()) { "color mismatch at $x,$y: ${b.getColor(x,y).toUInt().toString(16)} expected ${c.toArgb().toUInt().toString(16)}" } }
            expect(start,y1,outer)
            expect(start+width/2,y1+height/2,base)
            expect(start,y2,stroke)
            repeat(3) { expect(start+width/2,y2+it,stroke) }
            expect(start+width/2,y2+3,base)
            expect(start+width/2,y3+height/2,spill)
            expect(start,y3,outer)
            expect(end-1,y3,outer)
            for (x in start-3..end+2) {
                expect(x,y3-1,outer)
                expect(x,y3+height,outer)
            }
            for (y in y3..y3+height-1) {
                expect(start-1,y,outer)
                expect(end,y,outer)
            }
            expect(if (rtl) end-8 else start+8,y4+10,ink)
            expect(if (rtl) end-3 else start+2,y4+height+4+2,sentinel)
        }
        val cardOnlyX = if (rtl) start+3 else end-3
        tap(Offset(cardOnlyX.toFloat(), child.boundsInRoot.top+64))
        check(count == 0) { "card dispatched a nested action outside child" }
        tap(child.boundsInRoot.center)
        check(count == 1)
        println("PASS card RTL=$rtl dark=$dark width=$width: actual fill/stroke/no bleed, scoped ink restoration, static card plus sole host child action")
    } finally { scene.close(); surface.close() }
}

fun main(args: Array<String>) {
    val result = runCatching {
        val mode = args.singleOrNull()
        require(mode in setOf(null, "child-absent"))
        probe(false,false,98,mode)
        if (mode == null) probe(true,true,36,null)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if (result.isSuccess) 0 else 1)
}
