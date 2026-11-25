import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadListItem
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private class Host : PlatformContext by PlatformContext.Empty {
    val owners = mutableSetOf<SemanticsOwner>()
    override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
        override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
        override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
        override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
        override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
    }
    fun nodes(merged: Boolean = true) = owners.single().getAllSemanticsNodes(mergingEnabled = merged)
}

private fun SemanticsNode.words() = config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text }

@OptIn(InternalComposeUiApi::class)
private fun modes(rtl: Boolean, scale: Float, width: Int, mode: String?) {
    val host = Host()
    val scene = CanvasLayersComposeScene(density = Density(1f, scale), size = IntSize(180,420), platformContext = host)
    val surface = Surface.makeRasterN32Premul(180,420)
    val enabled = mutableStateOf(true)
    val calls = mutableListOf<Int>()
    val label = if (rtl) "رسائل جديدة ومحتوى يتطلب المراجعة" else "Important messages waiting for review"
    val help = if (rtl) "يمكن قراءة هذه المحادثة لاحقا في وضع عدم الاتصال" else "You may read this conversation later while offline"
    scene.setContent {
        MaterialTheme {
            CompositionLocalProvider(LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize().background(Color.White)) {
                    Column(Modifier.width(width.dp)) {
                        MadListItem(
                            if (mode == "blank") " " else label,
                            supportingText = if (mode == "blank-help") " " else help,
                            enabled = enabled.value,
                            onClick = { calls.add(17) },
                        )
                        MadListItem("Static", supportingText = "Status only", enabled = false)
                    }
                }
            }
        }
    }
    var t = 0L
    fun frame() {
        repeat(4) { surface.canvas.clear(0); scene.render(surface.canvas.asComposeCanvas(),t); t+=20_000_000 }
    }
    fun inspect(blocked: Boolean): SemanticsNode {
        val active = host.nodes().single { it.config.contains(SemanticsActions.OnClick) }
        check(active.words() == listOf(label, help))
        check(active.config.getOrNull(SemanticsProperties.Role) == Role.Button)
        check(active.config.contains(SemanticsProperties.Disabled) == blocked)
        check(host.nodes(false).count { it.config.contains(SemanticsActions.OnClick) } == 1)
        host.nodes().filter { it.words() in listOf(listOf("Static"),listOf("Status only")) }
            .also { check(it.size == 2 && it.all { n -> !n.config.contains(SemanticsActions.OnClick) &&
                !n.config.contains(SemanticsProperties.Role) && !n.config.contains(SemanticsProperties.Disabled) }) }
        check(active.boundsInRoot.width == width.toFloat() && active.boundsInRoot.height >= 48)
        val raw = host.nodes(false)
        val layouts = listOf(label,help).map { s ->
            val text = raw.single { s in it.words() }
            val l = mutableListOf<TextLayoutResult>()
            check(text.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(l))
            check(text.boundsInRoot.width > 0 && text.boundsInRoot.left >= active.boundsInRoot.left + 12 &&
                text.boundsInRoot.right <= active.boundsInRoot.right - 12)
            l.single()
        }
        check(layouts[0].lineCount == 2 && layouts[1].lineCount == 3)
        check(layouts.all { it.layoutInput.overflow == androidx.compose.ui.text.style.TextOverflow.Ellipsis && it.hasVisualOverflow && it.getLineEnd(it.lineCount - 1, visibleEnd = true) < it.layoutInput.text.length }) { layouts.map { listOf(it.hasVisualOverflow, it.isLineEllipsized(it.lineCount-1), it.getLineEnd(it.lineCount-1, visibleEnd=true), it.layoutInput.text.length) }.toString() }
        check(layouts[0].getParagraphDirection(0) == if (rtl) ResolvedTextDirection.Rtl else ResolvedTextDirection.Ltr)
        return active
    }
    fun tap(p: Offset) {
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=t/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=t/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    try {
        frame()
        val active = inspect(false)
        val a = active.boundsInRoot
        val ink = Bitmap().use { b ->
            b.allocN32Pixels(180,420)
            check(surface.readPixels(b,0,0))
            val inside = (a.top.toInt() until a.bottom.toInt()).sumOf { y ->
                (a.left.toInt()+12 until a.right.toInt()-12).count { x -> b.getColor(x,y) != -1 }
            }
            check(inside > 50)
            check((a.top.toInt() until a.bottom.toInt()).all { y ->
                (a.left.toInt() until a.left.toInt()+10).all { x -> b.getColor(x,y) == -1 } &&
                (a.right.toInt()-10 until a.right.toInt()).all { x -> b.getColor(x,y) == -1 }
            })
            inside
        }
        tap(a.center)
        tap(Offset(a.left+3,a.top+3))
        tap(Offset(a.right-3,a.bottom-3))
        check(calls == listOf(17,17,17))
        enabled.value = false
        frame()
        val disabled = inspect(true)
        tap(disabled.boundsInRoot.center)
        disabled.config[SemanticsActions.OnClick].action!!.invoke()
        frame()
        val static = host.nodes().single { it.words() == listOf("Static") }
        tap(static.boundsInRoot.center)
        check(calls == listOf(17,17,17))
        println("PASS row RTL=$rtl scale=$scale width=$width inkPixels=$ink height=${a.height}: label/support ellipsize without losing semantics, full hit area, static and disabled zero")
    } finally { scene.close(); surface.close() }
}

@OptIn(InternalComposeUiApi::class)
private fun lazy(indexKeys: Boolean) {
    val host = Host()
    val scene = CanvasLayersComposeScene(density = Density(1f), size = IntSize(200,260), platformContext = host)
    val surface = Surface.makeRasterN32Premul(200,260)
    val rows = mutableStateOf((1..80).toList())
    val actions = mutableListOf<Int>()
    var serials = 0
    lateinit var state: androidx.compose.foundation.lazy.LazyListState
    scene.setContent {
        state = rememberLazyListState()
        MaterialTheme {
            LazyColumn(Modifier.fillMaxSize(), state = state) {
                items(count = rows.value.size, key = { index -> if (indexKeys) index else rows.value[index] }) { i ->
                    val id = rows.value[i]
                    val marker = remember { ++serials }
                    MadListItem("Row $id", supportingText = "Host identity $marker", onClick = { actions += id })
                }
            }
        }
    }
    var t = 0L
    fun frame(count: Int = 8) {
        repeat(count) { surface.canvas.clear(0); scene.render(surface.canvas.asComposeCanvas(),t); t+=20_000_000 }
    }
    fun visible() = host.nodes().filter { n ->
        n.config.contains(SemanticsActions.OnClick) && n.boundsInRoot.height == n.size.height.toFloat()
    }.associateBy { it.words().first().removePrefix("Row ").toInt() }
    try {
        frame()
        val initial = state.firstVisibleItemIndex
        check(initial == 0 && visible().containsKey(1) && !visible().containsKey(50))
        repeat(7) {
            scene.sendPointerEvent(PointerEventType.Scroll,Offset(90f,120f),scrollDelta=Offset(0f,90f),
                timeMillis=t/1_000_000,type=PointerType.Mouse)
            frame()
        }
        check(state.firstVisibleItemIndex > 0 && !visible().containsKey(1)) { "Desktop wheel did not scroll lazy list" }
        val before = visible()
        val ids = before.values.sortedBy { it.boundsInRoot.top }.map { it.words().first().removePrefix("Row ").toInt() }
        check(ids.size >= 3)
        val a = ids[1]; val b = ids[2]
        rows.value = rows.value.map { if (it == a) b else if (it == b) a else it }
        frame()
        val after = visible()
        for (id in listOf(a,b)) {
            check(after.getValue(id).words()[1] == before.getValue(id).words()[1]) { "host keyed identity did not follow item" }
        }
        check(after.getValue(a).boundsInRoot.top > after.getValue(b).boundsInRoot.top)
        val target = after.getValue(a).boundsInRoot.center
        scene.sendPointerEvent(PointerEventType.Press,target,timeMillis=t/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,target,timeMillis=t/1_000_000+1,type=PointerType.Touch)
        frame()
        check(actions == listOf(a))
        println("PASS host lazy list real wheel firstVisible 0→${state.firstVisibleItemIndex}; stable keys retained item state after reorder and touch received ID $a")
    } finally { scene.close(); surface.close() }
}

fun main(args: Array<String>) {
    val mode = args.singleOrNull()
    val result = runCatching {
        require(mode in setOf(null, "blank", "blank-help", "index-keys", "rows-only"))
        if (mode in setOf(null, "blank", "blank-help", "rows-only")) modes(false,1f,88,mode)
        if (mode == null || mode == "rows-only") modes(true,1.6f,64,null)
        if (mode == null || mode == "index-keys") lazy(mode == "index-keys")
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if (result.isSuccess) 0 else 1)
}
