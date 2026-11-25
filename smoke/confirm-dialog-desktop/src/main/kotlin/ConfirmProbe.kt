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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadButton
import com.mad.maduikit.MadConfirmDialog
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun probe(mode:String?) {
    val width=520
    val height=360
    val size=IntSize(width,height)
    val owners=linkedSetOf<SemanticsOwner>()
    val host=object:PlatformContext by PlatformContext.Empty {
        override val windowInfo=object:WindowInfo {
            override val isWindowFocused=true
            override val containerSize=size
        }
        override val semanticsOwnerListener=object:PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner:SemanticsOwner){owners.add(semanticsOwner)}
            override fun onSemanticsOwnerRemoved(semanticsOwner:SemanticsOwner){owners.remove(semanticsOwner)}
            override fun onSemanticsChange(semanticsOwner:SemanticsOwner){}
            override fun onLayoutChange(semanticsOwner:SemanticsOwner,semanticsNodeId:Int){}
        }
    }
    val scene=CanvasLayersComposeScene(density=Density(1f),size=size,platformContext=host)
    val surface=Surface.makeRasterN32Premul(width,height)
    val visible=mutableStateOf(false)
    val policy=mutableStateOf<Pair<Boolean,Boolean>?>(if(mode=="outside-negative") true to true else null)
    val marker=Color(0xffa65979)
    val modalColor=Color(0xffc4eedf)
    val events=mutableListOf<String>()
    val theme=lightColorScheme(surfaceContainerHigh=modalColor)
    scene.setContent {
        MaterialTheme(colorScheme=theme) {
            Box(Modifier.fillMaxSize().background(marker)) {
                MadButton("Background", { events.add("background") }, Modifier.padding(8.dp))
                val title=if(mode=="blank-title") " " else "Archive project?"
                val message=if(mode=="blank-body") " " else "You can restore it later in settings."
                val confirm=if(mode=="blank-action") " " else "Archive"
                val cancel=if(mode=="duplicate-action") " Archive " else "Keep project"
                val explicit=policy.value
                if (explicit==null) {
                    MadConfirmDialog(visible.value,title,message,confirm,cancel,
                        {events.add("confirm")},{events.add("cancel")},{events.add("dismiss")})
                } else {
                    MadConfirmDialog(visible.value,title,message,confirm,cancel,
                        {events.add("confirm")},{events.add("cancel")},{events.add("dismiss")},
                        dismissOnBackPress=explicit.first,dismissOnClickOutside=explicit.second)
                }
            }
        }
    }
    var t=0L
    fun frame(){repeat(8){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),t);t+=20_000_000}}
    fun SemanticsNode.texts()=config.getOrNull(SemanticsProperties.Text).orEmpty().map{it.text}
    fun nodes(owner:SemanticsOwner,merged:Boolean=true)=owner.getAllSemanticsNodes(mergingEnabled=merged)
    fun tap(p:Offset){
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=t/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=t/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    fun escape(){
        scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyUp))
        frame()
    }
    fun modal(): Map<String,SemanticsNode> {
        check(owners.size==2) { "expected separate modal layer / semantics owner, got ${owners.size}" }
        val dialog=owners.single { o -> nodes(o).any { "Archive project?" in it.texts() } }
        val merged=nodes(dialog)
        val raw=nodes(dialog,false)
        check(merged.count { it.config.contains(SemanticsProperties.IsDialog) } == 1)
        check(merged.count { it.texts()==listOf("Archive project?") }==1)
        check(merged.count { it.texts()==listOf("You can restore it later in settings.") }==1)
        check(raw.count {it.config.contains(SemanticsActions.OnClick)}==2)
        val actions=merged.filter{it.config.contains(SemanticsActions.OnClick)}.associateBy{it.texts().single()}
        check(actions.keys==setOf("Archive","Keep project") && actions.values.all {
            it.config.getOrNull(SemanticsProperties.Role)==Role.Button
        })
        return actions
    }
    fun ink():Int {
        Bitmap().use { b ->
            b.allocN32Pixels(width,height)
            check(surface.readPixels(b,0,0))
            return (0 until height).sumOf { y -> (0 until width).count { x -> b.getColor(x,y)==modalColor.toArgb() } }
        }
    }
    try {
        frame()
        val root=owners.single()
        val base=nodes(root).single{"Background" in it.texts()}.boundsInRoot.center
        check(ink()==0)
        tap(base)
        check(events==listOf("background"))
        visible.value=true;frame()
        val actions=modal()
        val pixels=ink()
        check(pixels>5000)
        tap(base)
        check(events==listOf("background")) { "outside tap violated default modal policy" }
        tap(actions.getValue("Archive").boundsInRoot.center)
        check(events==listOf("background","confirm"))
        tap(modal().getValue("Keep project").boundsInRoot.center)
        check(events==listOf("background","confirm","cancel"))
        modal() // neither action closes the host-controlled modal
        escape()
        check(events==listOf("background","confirm","cancel","dismiss")) { "Desktop Escape did not request exactly one dismissal" }
        modal()
        policy.value=false to true;frame()
        escape()
        check(events.size==4)
        tap(base)
        check(events==listOf("background","confirm","cancel","dismiss","dismiss")) { "outside must dismiss without click-through" }
        modal()
        visible.value=false;frame()
        check(owners.single()===root && ink()==0)
        tap(base)
        check(events.last()=="background" && events.size==6)
        println("PASS actual dialog layer, ${pixels} modal-colored Skia pixels, one dialog/two named actions, no click-through, confirm/cancel/dismiss routing and host-controlled close; Desktop Escape/outside policy")
    } finally {scene.close();surface.close()}
}

fun main(args:Array<String>) {
    val result=runCatching { probe(args.singleOrNull()) }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
