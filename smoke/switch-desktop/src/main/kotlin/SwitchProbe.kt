import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadSwitch
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun runProbe(args: Array<String>) {
    val mode = args.singleOrNull()
    require(mode in setOf(null, "roleless", "long", "blank"))
    val roleless = mode == "roleless"
    val owners = mutableSetOf<SemanticsOwner>()
    val host = object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener = object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene = CanvasLayersComposeScene(density = Density(1f), size = IntSize(260, 110), platformContext = host)
    val surface = Surface.makeRasterN32Premul(260, 110)
    val checked = mutableStateOf(false)
    val enabled = mutableStateOf(true)
    var accept = false
    val requests = mutableListOf<Boolean>()
    val onChanged: (Boolean) -> Unit = { requests += it; if (accept) checked.value = it }
    val label = when(mode) {
        "long" -> "Synchronize all synthetic library notifications across our public test devices"
        "blank" -> "  "
        else -> "Sync library"
    }
    scene.setContent {
        MaterialTheme {
            if (roleless) Row(Modifier.toggleable(checked.value, enabled = enabled.value, onValueChange = onChanged)) {
                Text(label)
                Switch(checked.value, onCheckedChange = null)
            } else MadSwitch(label, checked.value, onChanged, enabled = enabled.value)
        }
    }
    var now = 0L
    fun frame() { scene.render(surface.canvas.asComposeCanvas(), now); now += 20_000_000 }
    fun state(expected: ToggleableState, disabled: Boolean): SemanticsNode {
        val all = owners.single().getAllSemanticsNodes(mergingEnabled = true)
        val action = all.single { it.config.contains(SemanticsActions.OnClick) }
        check(action.config[SemanticsProperties.Text].map { it.text } == listOf(label)) { "label mismatch" }
        check(action.config[SemanticsProperties.ToggleableState] == expected) { "controlled state mismatch" }
        check(action.config.contains(SemanticsProperties.Disabled) == disabled) { "disabled mismatch" }
        check(action.config.getOrNull(SemanticsProperties.Role) == Role.Switch) { "Role.Switch required on named action" }
        val raw = owners.single().getAllSemanticsNodes(mergingEnabled = false)
        check(raw.count { it.config.contains(SemanticsActions.OnClick) } == 1) { "duplicate raw action" }
        return action
    }
    fun tap(x: Float, y: Float) {
        scene.sendPointerEvent(PointerEventType.Press, Offset(x, y), timeMillis = now / 1_000_000, type = PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release, Offset(x, y), timeMillis = now / 1_000_000 + 1, type = PointerType.Touch)
        repeat(3) { frame() }
    }
    try {
        repeat(4) { frame() }
        val bounds = state(ToggleableState.Off, false).boundsInRoot
        check(bounds.height >= 48f) { "minimum row height not applied" }
        if (mode == "long") {
            val raw = owners.single().getAllSemanticsNodes(mergingEnabled = false)
            val text = raw.single { it.config.getOrNull(SemanticsProperties.Text)?.singleOrNull()?.text == label }
            check(text.boundsInRoot.height > 30f) { "long label did not wrap" }
            check(bounds.right - text.boundsInRoot.right >= 60f) { "switch display region squeezed by label" }
            println("PASS long label wraps and preserves switch region")
        }
        val labelX = bounds.left + 20f
        val graphicX = bounds.right - 24f
        val y = bounds.center.y
        tap(labelX, y)
        check(requests == listOf(true))
        state(ToggleableState.Off, false)
        accept = true
        requests.clear()
        tap(graphicX, y)
        check(requests == listOf(true))
        state(ToggleableState.On, false)
        enabled.value = false
        repeat(3) { frame() }
        state(ToggleableState.On, true)
        tap(labelX, y); tap(graphicX, y)
        check(requests == listOf(true))
        println("PASS merged=1 rawActions=1 label=1 graphic=1 declined=off accepted=on disabled=0")
    } finally { scene.close(); surface.close() }
}

fun main(args: Array<String>) {
    val result = runCatching { runProbe(args) }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if (result.isSuccess) 0 else 1)
}
