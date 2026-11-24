import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.*
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun probe(mode: String?) {
    check(mode in listOf(null, "blank-label", "blank-loading"))
    val owners = mutableSetOf<SemanticsOwner>()
    val host = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene = CanvasLayersComposeScene(density = Density(1f), size = IntSize(220, 320), platformContext = host)
    val surface = Surface.makeRasterN32Premul(220, 320)
    val enabled = mutableStateOf(true)
    val legacyEnabled = mutableStateOf(true)
    val loading = mutableStateOf<String?>(if (mode == "blank-loading") " \t" else null)
    val localized = "正在保存"
    val calls = mutableListOf<String>()
    val theme = lightColorScheme(primary = Color(0xff2244aa), error = Color(0xffba1234), outline = Color(0xff4b4b4b))
    val text = "Save"
    scene.setContent {
        MaterialTheme(colorScheme = theme) {
            Column(Modifier.fillMaxSize().background(Color.White)) {
                MadButton(text, { calls += "legacy" }, Modifier.width(170.dp), legacyEnabled.value)
                MadButtonVariant.entries.forEach { variant ->
                    MadActionButton(
                        label = if (mode == "blank-label") " " else text,
                        variant = variant,
                        onClick = { calls += variant.name },
                        modifier = Modifier.width(170.dp),
                        enabled = enabled.value,
                        loadingStatus = loading.value,
                    )
                }
            }
        }
    }
    var t = 0L
    fun frame() {
        repeat(3) {
            surface.canvas.clear(0)
            scene.render(surface.canvas.asComposeCanvas(), t)
            t += 20_000_000
        }
    }
    fun actions(state: String?, blocked: Boolean, legacyBlocked: Boolean = false): List<SemanticsNode> {
        val owner = owners.single()
        val raw = owner.getAllSemanticsNodes(mergingEnabled = false)
        val buttons = owner.getAllSemanticsNodes(mergingEnabled = true)
            .filter { it.config.getOrNull(SemanticsProperties.Role) == Role.Button }.sortedBy { it.boundsInRoot.top }
        check(buttons.size == 5 && raw.count { it.config.contains(SemanticsActions.OnClick) } == 5)
        check(raw.none { it.config.contains(SemanticsProperties.ProgressBarRangeInfo) })
        check(raw.none { it.config.contains(SemanticsProperties.ContentDescription) })
        check(raw.count { it.config.contains(SemanticsProperties.StateDescription) } == if (state == null) 0 else 4)
        buttons.forEachIndexed { i, node ->
            check(node.config[SemanticsProperties.Text].map { it.text } == listOf(text))
            check(node.config.getOrNull(SemanticsProperties.StateDescription) == if (i == 0) null else state)
            check(node.config.contains(SemanticsProperties.Disabled) == if (i == 0) legacyBlocked else blocked)
            check(node.config.contains(SemanticsActions.OnClick))
        }
        return buttons
    }
    fun tap(p: Offset) {
        scene.sendPointerEvent(PointerEventType.Press, p, timeMillis = t/1_000_000, type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release, p, timeMillis = t/1_000_000+1, type = PointerType.Touch)
        frame()
    }
    try {
        frame()
        val nodes = actions(null, false)
        Bitmap().use { bitmap ->
            bitmap.allocN32Pixels(220,320)
            check(surface.readPixels(bitmap,0,0))
            fun pixel(x: Int, y: Int) = bitmap.getColor(x,y)
            val centers = nodes.map { it.boundsInRoot.center.y.toInt() }
            val fills = centers.map { pixel(28,it) }
            check(fills == listOf(0xff2244aa.toInt(), 0xff2244aa.toInt(), -1, -1, 0xffba1234.toInt())) { "unexpected fills $fills" }
            fun nonWhiteLeft(i: Int) = (0..28).count { pixel(it, centers[i]) != -1 }
            val outlineCounts = listOf(nonWhiteLeft(2), nonWhiteLeft(3))
            check(outlineCounts[0] > 0 && outlineCounts[1] == 0) { "secondary/text contrast $outlineCounts" }
            val old = nodes[0].boundsInRoot
            val primary = nodes[1].boundsInRoot
            check(old.width == primary.width && old.height == primary.height)
            val delta = primary.top.toInt() - old.top.toInt()
            check((old.top.toInt() until old.bottom.toInt()).all { y ->
                (0 until 170).all { x -> pixel(x,y) == pixel(x,y+delta) }
            }) { "new primary should match legacy pixels when idle" }
            println("PASS idle variants (legacy/primary exact same pixels, outlined vs text edge counts $outlineCounts, distinct danger fill)")
        }
        nodes.forEach { tap(it.boundsInRoot.center) }
        val expected = listOf("legacy", "Primary", "Secondary", "Text", "Danger")
        check(calls == expected)
        loading.value = localized
        frame()
        fun ignoreNew(state: String?) {
            actions(state,true).drop(1).forEach {
                tap(it.boundsInRoot.center)
                it.config[SemanticsActions.OnClick].action!!.invoke()
                frame()
            }
            check(calls == expected) { "blocked button dispatched" }
        }
        ignoreNew(localized) // enabled + loading
        enabled.value = false
        frame()
        ignoreNew(localized) // disabled + loading retains state description
        loading.value = null
        frame()
        ignoreNew(null) // disabled alone
        enabled.value = true
        legacyEnabled.value = false
        frame()
        val recovered = actions(null,false,true)
        tap(recovered.first().boundsInRoot.center)
        check(calls == expected)
        tap(recovered.last().boundsInRoot.center)
        check(calls == expected + "Danger")
        println("PASS old label/action, four one-action buttons, host loading precedence, no spinner name/progress semantics, disabled/loading pointer and direct action zero, recovery")
    } finally { scene.close(); surface.close() }
}

fun main(args: Array<String>) {
    val result = runCatching { probe(args.singleOrNull()) }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if (result.isSuccess) 0 else 1)
}
