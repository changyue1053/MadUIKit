import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadIcon
import com.mad.maduikit.MadIconButton
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun runProbe(args: Array<String>) {
    val mode=args.singleOrNull()
    require(mode in setOf(null,"blank-icon","blank-action","duplicate-label"))
    val owners=mutableSetOf<SemanticsOwner>()
    val host=object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener=object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene=CanvasLayersComposeScene(density=Density(1f),size=IntSize(220,150),platformContext=host)
    val surface=Surface.makeRasterN32Premul(220,150)
    val painter=ColorPainter(Color.Blue)
    val enabled=mutableStateOf(true)
    var count=0
    val label="Refresh library"
    val meaning="Public library state"
    scene.setContent {
        MaterialTheme {
            Column {
                MadIcon(painter, contentDescription=null, modifier=Modifier.size(24.dp))
                MadIcon(painter, contentDescription=if(mode=="blank-icon") " " else meaning)
                MadIconButton(painter, label=if(mode=="blank-action") "\t" else label, onClick={count++}, enabled=enabled.value)
                if(mode=="duplicate-label") MadIcon(painter, contentDescription=label)
            }
        }
    }
    var n=0L
    fun frame(){ scene.render(surface.canvas.asComposeCanvas(),n); n+=20_000_000 }
    fun action(disabled:Boolean): SemanticsNode {
        val owner=owners.single()
        val merged=owner.getAllSemanticsNodes(mergingEnabled=true)
        val raw=owner.getAllSemanticsNodes(mergingEnabled=false)
        val target=merged.single { it.config.contains(SemanticsActions.OnClick) }
        check(target.config.getOrNull(SemanticsProperties.Role)==Role.Button)
        check(target.config[SemanticsProperties.ContentDescription]==listOf(label))
        check(target.config.contains(SemanticsProperties.Disabled)==disabled)
        check(raw.count { it.config.contains(SemanticsActions.OnClick) }==1)
        check(raw.single { it.config.getOrNull(SemanticsProperties.ContentDescription)==listOf(meaning) }
            .config.getOrNull(SemanticsProperties.Role)==Role.Image)
        val labels=raw.flatMap { it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty() }
        check(labels.sorted()==listOf(meaning,label).sorted()) { "unexpected duplicate or decorative label" }
        check(target.boundsInRoot.width>=48f && target.boundsInRoot.height>=48f)
        return target
    }
    fun tap(p:Offset) {
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=n/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=n/1_000_000+1,type=PointerType.Touch)
        repeat(3){frame()}
    }
    try {
        repeat(4){frame()}
        val bounds=action(false).boundsInRoot
        tap(bounds.center)
        check(count==1)
        tap(Offset(bounds.left+3,bounds.top+3))
        check(count==2) { "48dp outer hit region did not invoke action" }
        enabled.value=false
        repeat(3){frame()}
        val disabled=action(true).boundsInRoot
        tap(disabled.center)
        tap(Offset(disabled.left+3,disabled.top+3))
        check(count==2) { "disabled action fired" }
        println("PASS icon roles: decorative=0 named standalone=1 merged button=1 raw action=1 center=1 outer=1 disabled=0")
    } finally { scene.close(); surface.close() }
}
fun main(args:Array<String>) {
    val result=runCatching { runProbe(args) }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
