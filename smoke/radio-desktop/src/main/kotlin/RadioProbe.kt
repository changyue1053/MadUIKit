import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
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

import androidx.compose.ui.unit.*
import com.mad.maduikit.MadRadio
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
    val selected = mutableStateOf(0)
    val enabled = mutableStateOf(true)
    val requests = mutableListOf<Int>()
    val label = when (mode) {
        "blank" -> " \t"
        "narrow" -> "Read this article in offline mode"
        else -> "Offline"
    }
    scene.setContent {
        MaterialTheme {
            Column(Modifier.width(if (mode == "narrow") 140.dp else 200.dp).selectableGroup()) {
                MadRadio(label = "Online", selected = selected.value == 0, onSelect = { requests.add(0) })
                MadRadio(label = label, selected = selected.value == 1, onSelect = { requests.add(1) },
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
        check(merged.count { it.config.contains(SemanticsProperties.SelectableGroup) } == 1)
        val choices = merged.filter { it.config.getOrNull(SemanticsProperties.Role) == Role.RadioButton }
        check(choices.size == 2)
        fun names(it: SemanticsNode) = it.config.getOrNull(SemanticsProperties.Text)?.map { it.text }
        val target = choices.single { names(it) == listOf(label) }
        val sibling = choices.single { names(it) == listOf("Online") }
        check(target.config.getOrNull(SemanticsProperties.Selected) == expected)
        check(sibling.config.getOrNull(SemanticsProperties.Selected) == !expected)
        check(target.config.contains(SemanticsProperties.Disabled) == disabled)
        check(choices.all { it.config.contains(SemanticsActions.OnClick) })
        check(raw.count { it.config.contains(SemanticsActions.OnClick) } == 2)
        check(raw.count { it.config.contains(SemanticsProperties.Selected) } == 2)
        check(raw.flatMap { names(it).orEmpty() }.sorted() == listOf("Online", label).sorted())
        check(raw.none { it.config.contains(SemanticsProperties.ContentDescription) })
        check(target.boundsInRoot.width >= 48f && target.boundsInRoot.height >= 48f)
        if (mode == "narrow") {
            val getLayout = raw.single { names(it) == listOf(label) }
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
        check(requests == listOf(1))
        val unchanged = inspect(false, false)
        tap(Offset(unchanged.boundsInRoot.left + 3, unchanged.boundsInRoot.top + 3))
        check(requests == listOf(1, 1))
        inspect(false, false)
        check(unchanged.config[SemanticsActions.OnClick].action!!.invoke())
        render()
        check(requests == listOf(1, 1, 1))
        inspect(false, false)
        selected.value = 1
        render()
        val chosen = inspect(true, false)
        tap(chosen.boundsInRoot.center)
        tap(Offset(chosen.boundsInRoot.left + 3, chosen.boundsInRoot.top + 3))
        chosen.config[SemanticsActions.OnClick].action!!.invoke()
        render()
        check(requests == listOf(1, 1, 1)) { "selected row requested again" }
        inspect(true, false)
        selected.value = 0
        enabled.value = false
        render()
        val disabledTarget = inspect(false, true)
        val disabled = disabledTarget.boundsInRoot
        tap(disabled.center)
        tap(Offset(disabled.left + 3, disabled.top + 3))
        disabledTarget.config[SemanticsActions.OnClick].action!!.invoke()
        render()
        check(requests == listOf(1, 1, 1)) { "disabled unselected requested selection" }
        selected.value = 1
        render()
        inspect(true, true)
        println("PASS radio desktop (${mode ?: "default"}): group, one name/role/action per row, center + inset, host-controlled exclusivity, selected/disabled zero")
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
