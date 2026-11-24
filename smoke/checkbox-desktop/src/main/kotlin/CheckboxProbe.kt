import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadCheckbox
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun runProbe(mode: String?) {
    require(mode in setOf(null, "blank", "narrow"))
    val owners = mutableSetOf<SemanticsOwner>()
    val host = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene = CanvasLayersComposeScene(density = Density(1f), size = IntSize(220, 240), platformContext = host)
    val surface = Surface.makeRasterN32Premul(220, 240)
    val checked = mutableStateOf(false)
    val enabled = mutableStateOf(true)
    val requests = mutableListOf<Boolean>()
    val label = when (mode) {
        "blank" -> " \t"
        "narrow" -> "Save this article for offline reading"
        else -> "Save article"
    }
    scene.setContent {
        MaterialTheme {
            Box(Modifier.width(if (mode == "narrow") 140.dp else 200.dp)) {
                MadCheckbox(label = label, checked = checked.value, onCheckedChange = { requests.add(it) },
                    enabled = enabled.value)
            }
        }
    }
    var n = 0L
    fun render() {
        repeat(3) { scene.render(surface.canvas.asComposeCanvas(), n); n += 20_000_000 }
    }
    fun inspect(expected: Boolean, disabled: Boolean): SemanticsNode {
        val owner = owners.single()
        val merged = owner.getAllSemanticsNodes(mergingEnabled = true)
        val raw = owner.getAllSemanticsNodes(mergingEnabled = false)
        val target = merged.single { it.config.getOrNull(SemanticsProperties.Role) == Role.Checkbox }
        check(target.config.getOrNull(SemanticsProperties.Text)?.map { it.text } == listOf(label))
        check(target.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().isEmpty())
        check(target.config.getOrNull(SemanticsProperties.ToggleableState) ==
            if (expected) ToggleableState.On else ToggleableState.Off)
        check(target.config.contains(SemanticsProperties.Disabled) == disabled)
        check(target.config.contains(SemanticsActions.OnClick))
        check(raw.count { it.config.contains(SemanticsActions.OnClick) } == 1)
        check(raw.count { it.config.contains(SemanticsProperties.ToggleableState) } == 1)
        check(raw.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map { it.text } == listOf(label))
        check(raw.none { it.config.contains(SemanticsProperties.ContentDescription) })
        check(target.boundsInRoot.width >= 48f && target.boundsInRoot.height >= 48f) { "touch target smaller than 48 dp" }
        if (mode == "narrow") {
            val getLayout = raw.single { it.config.contains(SemanticsProperties.Text) }
                .config[SemanticsActions.GetTextLayoutResult].action!!
            val layouts = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
            check(getLayout(layouts) && layouts.single().lineCount > 1 && !layouts.single().hasVisualOverflow)
        }
        return target
    }
    fun tap(p: Offset) {
        scene.sendPointerEvent(PointerEventType.Press, p, timeMillis = n / 1_000_000, type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release, p, timeMillis = n / 1_000_000 + 1, type = PointerType.Touch)
        render()
    }
    try {
        render()
        val first = inspect(false, false)
        tap(first.boundsInRoot.center)
        check(requests == listOf(true))
        val unchanged = inspect(false, false)
        tap(Offset(unchanged.boundsInRoot.left + 3, unchanged.boundsInRoot.top + 3))
        check(requests == listOf(true, true)) { "outer hit did not request host change" }
        inspect(false, false)
        checked.value = true
        render()
        val selected = inspect(true, false)
        val click = selected.config[SemanticsActions.OnClick].action!!
        check(click())
        render()
        check(requests == listOf(true, true, false))
        inspect(true, false)
        enabled.value = false
        render()
        val disabledTarget = inspect(true, true)
        val disabled = disabledTarget.boundsInRoot
        tap(disabled.center)
        tap(Offset(disabled.left + 3, disabled.top + 3))
        disabledTarget.config[SemanticsActions.OnClick].action!!.invoke()
        render()
        check(requests == listOf(true, true, false)) { "disabled pointer or semantics invoked callback" }
        checked.value = false
        render()
        inspect(false, true)
        check(requests == listOf(true, true, false))
        println("PASS checkbox desktop (${mode ?: "default"}): one name/role/action, center + inset, controlled both states, disabled pointer and semantics zero")
    } finally {
        scene.close()
        surface.close()
    }
}

fun main(args: Array<String>) {
    val result = runCatching { runProbe(args.singleOrNull()) }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if (result.isSuccess) 0 else 1)
}
