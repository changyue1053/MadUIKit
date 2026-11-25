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
import com.mad.maduikit.MadBottomSheet
import com.mad.maduikit.MadButton
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun probe(width:Int,height:Int,mode:String?){
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
    val marker=Color(0xffa65979)
    val sheetColor=Color(0xffb4f0d4)
    val title="Transfer information"
    val message=(1..52).joinToString("\n") { "File $it remains available in the workspace." }
    var windowHits=0
    var closeRequests=0
    scene.setContent {
        MaterialTheme(colorScheme=lightColorScheme(surfaceContainerLow=sheetColor)) {
            Box(Modifier.fillMaxSize().background(marker)) {
                MadButton("Background", {windowHits++}, Modifier.padding(4.dp))
                MadBottomSheet(visible.value,
                    if(mode=="blank-title") " " else title,
                    if(mode=="blank-message") " " else message,
                    if(mode=="blank-close") " " else "Close details",
                    onCloseRequest={
                        closeRequests++
                        if(mode=="auto-close") visible.value=false
                    })
            }
        }
    }
    var t=0L
    fun frame(count:Int=10){
        repeat(count){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),t);t+=20_000_000L}
    }
    fun settle(){
        frame(6); Thread.sleep(140); frame(48)
    }
    fun tap(p:Offset){
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=t/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=t/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    fun escape(){
        scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyDown))
        scene.sendKeyEvent(KeyEvent(Key.Escape,KeyEventType.KeyUp));frame()
    }
    fun SemanticsNode.texts()=config.getOrNull(SemanticsProperties.Text).orEmpty().map{it.text}
    fun nodes(o:SemanticsOwner)=o.getAllSemanticsNodes(mergingEnabled=true)
    fun layer():SemanticsOwner{
        check(owners.size==2){ "expected actual sheet layer" }
        return owners.single{o->nodes(o).any{title in it.texts()}}
    }
    fun raster():Pair<List<Int>,Int> {
        Bitmap().use { b->
            b.allocN32Pixels(width,height);check(surface.readPixels(b,0,0))
            val rows=(0 until height).map{b.getColor(width/2,it)}
            val extent=rows.indexOf(sheetColor.toArgb()).let { if(it<0) 0 else height-it }
            return rows to extent
        }
    }
    try{
        frame()
        val root=owners.single()
        val bottomButton=nodes(root).single{"Background" in it.texts()}.boundsInRoot.center
        tap(bottomButton);check(windowHits==1)
        check(raster().second==0)
        visible.value=true;settle()
        val modal=layer()
        check(nodes(modal).count{it.config.contains(SemanticsProperties.IsDialog)}==1)
        check(nodes(modal).single{it.config.contains(SemanticsProperties.Heading)}.texts()==listOf(title))
        val action=nodes(modal).single{it.config.contains(SemanticsActions.OnClick)}
        check(action.texts()==listOf("Close details") && action.config[SemanticsProperties.Role]==Role.Button)
        check(action.boundsInRoot.height>=48 && action.boundsInRoot.bottom<=height)
        val body=nodes(modal).single{it.config.contains(SemanticsProperties.VerticalScrollAxisRange)}
        check(body.boundsInRoot.height >= 35)
        val (open,sheetPx)=raster()
        check(sheetPx in 100..minOf(480,height)) { "unexpected expanded extent $sheetPx" }
        check(open[0]!=marker.toArgb() || sheetPx==height) // scrim in uncovered portion
        check(open.last()==sheetColor.toArgb())
        tap(bottomButton);escape()
        check(windowHits==1 && closeRequests==0 && owners.size==2)
        val range=body.config[SemanticsProperties.VerticalScrollAxisRange]
        check(range.value()==0f && range.maxValue()>0f)
        val point=body.boundsInRoot.center
        scene.sendPointerEvent(PointerEventType.Scroll,point,scrollDelta=Offset(0f,5f),type=PointerType.Mouse,
            timeMillis=t/1_000_000)
        settle()
        check(range.value()>0f) { "body did not scroll" }
        val after=nodes(modal).single{it.config.contains(SemanticsActions.OnClick)}
        check(after.boundsInRoot==action.boundsInRoot) { "scroll moved fixed close action" }
        tap(after.boundsInRoot.center)
        check(closeRequests==1)
        check(visible.value && owners.size==2 && layer()===modal && raster().second==sheetPx) { "sheet closed before host acceptance" }
        visible.value=false;settle()
        check(owners.single()===root && raster().second==0)
        after.config[SemanticsActions.OnClick].action!!.invoke();frame()
        check(closeRequests==1) { "disposed close callback dispatched" }
        tap(bottomButton);check(windowHits==2)
        println("PASS real expanded modal layer ${width}×$height: $sheetPx px bounded, wheel scroll=${range.value()}, close remains fixed, backdrop/Escape blocked, one close request held until host unmount, no stale callback")
    } finally {scene.close();surface.close()}
}

fun main(args:Array<String>){
    val result=runCatching {
        val mode=args.singleOrNull()
        require(mode in setOf(null,"blank-title","blank-message","blank-close","auto-close"))
        probe(520,660,mode)
        if(mode==null)probe(300,226,null)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
