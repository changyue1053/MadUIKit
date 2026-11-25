import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
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
private fun probe(rtl: Boolean, scale: Float, width: Int, mode: String?, five: Boolean = false) {
    val owners = mutableSetOf<SemanticsOwner>()
    val host = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene = CanvasLayersComposeScene(density=Density(1f,scale), size=IntSize(364,170), platformContext=host)
    val surface = Surface.makeRasterN32Premul(364,170)
    val painter = ColorPainter(Color(0xff164b44))
    val indicator = Color(0xffb7ecc2)
    val base = Color(0xfff9f6e9)
    val theme = lightColorScheme(secondaryContainer=indicator, surfaceContainer=base)
    val entries = listOf(
        MadNavigationItem(if(mode=="blank-id") " " else "inbox", if(rtl) "الوارد" else "Inbox", painter),
        MadNavigationItem(if(mode=="duplicate") "inbox" else "browse",
            if(mode=="blank-label") " " else if(rtl) "استعراض المحادثات السابقة" else "Browse and discover archived conversations", painter),
        MadNavigationItem("profile", if(rtl) "الملف" else "Profile", painter,false),
    )
    val expanded = if (five) entries + listOf(
        MadNavigationItem("settings", "Einstellungen und Benachrichtigungen", painter),
        MadNavigationItem("help", "مركز المساعدة والردود على الأسئلة", painter, false),
    ) else entries
    val labels=expanded.associate { it.label to it.id }
    val items=mutableStateOf(if(mode=="count") expanded.take(1) else expanded)
    val selected=mutableStateOf(if(mode=="unknown") "missing" else "inbox")
    val requests=mutableListOf<String>()
    scene.setContent {
        MaterialTheme(colorScheme=theme) {
            CompositionLocalProvider(LocalLayoutDirection provides if(rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    MadNavigationBar(items.value, selected.value, { requests.add(it) }, Modifier.width(width.dp))
                }
            }
        }
    }
    var now=0L
    fun frame() { repeat(4) { surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),now); now+=20_000_000 } }
    fun tap(where: Offset) {
        scene.sendPointerEvent(PointerEventType.Press,where,timeMillis=now/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,where,timeMillis=now/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    fun action(node:SemanticsNode) { node.config[SemanticsActions.OnClick].action!!.invoke(); frame() }
    fun inspect(current: String, expectedOrder: List<String>): Map<String,SemanticsNode> {
        val owner=owners.single()
        val merged=owner.getAllSemanticsNodes(mergingEnabled=true)
        val raw=owner.getAllSemanticsNodes(mergingEnabled=false)
        check(merged.count { it.config.contains(SemanticsProperties.SelectableGroup) }==1)
        check(raw.count { it.config.contains(SemanticsActions.OnClick) }==expanded.size)
        check(raw.none { it.config.contains(SemanticsProperties.ContentDescription) })
        val tabs=merged.filter { it.config.getOrNull(SemanticsProperties.Role)==Role.Tab }.associateBy {
            labels.getValue(it.config[SemanticsProperties.Text].single().text)
        }
        check(tabs.keys == expanded.map { it.id }.toSet())
        if (five) {
            val longLabels = expanded.filter { it.id in setOf("browse","settings","help") }.map { it.label }
            for (label in longLabels) {
                val node = raw.single { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == label }
                val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                val measure = node.config[SemanticsActions.GetTextLayoutResult].action!!
                check(measure(results))
                val text = results.single()
                check(text.lineCount == 2 && text.hasVisualOverflow &&
                    text.getLineEnd(1, visibleEnd = true) < label.length)
            }
        }
        check(tabs.values.all{it.boundsInRoot.height>=64 && it.boundsInRoot.width>=48})
        check(tabs.values.map{it.boundsInRoot.height}.distinct().size == 1)
        check(tabs.values.sumOf{it.boundsInRoot.width.toDouble()} == width.toDouble())
        check(tabs.entries.sortedBy{it.value.boundsInRoot.left}.map{it.key} == if(rtl) expectedOrder.reversed() else expectedOrder)
        check(tabs.filterValues{it.config[SemanticsProperties.Selected]}.keys == setOf(current))
        check(tabs.filterValues{it.config.contains(SemanticsProperties.Disabled)}.keys == expanded.filterNot { it.enabled }.map { it.id }.toSet())
        return tabs
    }
    fun raster(nodes: Map<String,SemanticsNode>, current: String) {
        Bitmap().use { b ->
            b.allocN32Pixels(364,170); check(surface.readPixels(b,0,0))
            val chosen=nodes.getValue(current).boundsInRoot
            fun fill(r: androidx.compose.ui.geometry.Rect)=
                (r.top.toInt() until r.bottom.toInt()).sumOf { y ->
                    (r.left.toInt() until r.right.toInt()).count { x -> b.getColor(x,y)==indicator.toArgb() }
                }
            check(fill(chosen)>200 && nodes.filterKeys { it != current }.values.all { fill(it.boundsInRoot)==0 })
        }
    }
    try {
        frame()
        val initialOrder=expanded.map { it.id }
        val initial=inspect("inbox",initialOrder)
        raster(initial,"inbox")
        val browse=initial.getValue("browse").boundsInRoot
        tap(browse.center)
        tap(Offset(browse.left+2,browse.bottom-2))
        check(requests==listOf("browse","browse")) { "unaccepted requests mutated or deduped host selection" }
        val unchanged=inspect("inbox",initialOrder)
        val disabled=unchanged.getValue("profile")
        tap(disabled.boundsInRoot.center)
        action(disabled)
        tap(unchanged.getValue("inbox").boundsInRoot.center)
        action(unchanged.getValue("inbox"))
        if (five) {
            val extra = unchanged.getValue("help")
            tap(extra.boundsInRoot.center)
            action(extra)
        }
        check(requests==listOf("browse","browse")) { "disabled/current item dispatched" }
        selected.value="browse"
        items.value=expanded.reversed()
        frame()
        val reverse=initialOrder.reversed()
        val accepted=inspect("browse",reverse)
        raster(accepted,"browse")
        action(accepted.getValue("inbox"))
        check(requests==listOf("browse","browse","inbox"))
        selected.value="profile" // current route may be disabled during a host transition
        frame()
        val currentDisabled=inspect("profile",reverse).getValue("profile")
        action(currentDisabled)
        check(requests==listOf("browse","browse","inbox"))
        println("PASS navigation RTL=$rtl fontScale=$scale width=$width destinations=${expanded.size}: one labeled tab per ID, actual selection raster, repeat intent/host acceptance, no selected or disabled action, stable reordering")
    } finally { scene.close(); surface.close() }
}

fun main(args: Array<String>) {
    val result=runCatching {
        val mode=args.singleOrNull()
        require(mode in setOf(null,"count","duplicate","unknown","blank-id","blank-label"))
        probe(false,1f,270,mode)
        if(mode==null) {
            probe(true,1.6f,144,null)
            probe(true,1.6f,240,null,five=true)
        }
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
