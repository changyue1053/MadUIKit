import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.*
import com.mad.maduikit.*
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun probe(rtl:Boolean,width:Int,scale:Float,mode:String?) {
    val owners=mutableSetOf<SemanticsOwner>()
    val host=object:PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener=object:PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner:SemanticsOwner){owners.add(semanticsOwner)}
            override fun onSemanticsOwnerRemoved(semanticsOwner:SemanticsOwner){owners.remove(semanticsOwner)}
            override fun onSemanticsChange(semanticsOwner:SemanticsOwner){}
            override fun onLayoutChange(semanticsOwner:SemanticsOwner,semanticsNodeId:Int){}
        }
    }
    val size=IntSize(380,680)
    val scene=CanvasLayersComposeScene(density=Density(1f,scale),size=size,platformContext=host)
    val surface=Surface.makeRasterN32Premul(size.width,size.height)
    val state=mutableStateOf(MadPanelState.Loading)
    val hasAction=mutableStateOf(true)
    val generation=mutableStateOf(1)
    val calls=mutableListOf<String>()
    val paper=Color(if(rtl) 0xff172b32 else 0xfff5faf6)
    val error=Color(if(rtl) 0xffffbcb5 else 0xffa00224)
    val primary=Color(if(rtl) 0xff99d7b0 else 0xff075e43)
    val outside=Color(0xffce639c)
    val theme=if(rtl) darkColorScheme(surfaceContainer=paper,error=error,primary=primary)
       else lightColorScheme(surfaceContainer=paper,error=error,primary=primary)
    fun title(at:MadPanelState)=when(at) {
        MadPanelState.Loading -> if(rtl) "جارٍ تحميل نتائج المساحة المشتركة" else "Loading workspace results"
        MadPanelState.Empty -> if(rtl) "لا توجد نتائج في مساحة العمل الحالية" else "No results in this workspace"
        MadPanelState.Error -> if(rtl) "تعذر تحميل نتائج مساحة العمل الحالية" else "The workspace results could not load"
    }
    val message=if(rtl) "راجع الإعدادات ثم عد إلى هذه الصفحة." else "Review the settings and return to this page."
    val action=if(rtl) "إعادة محاولة تحميل النتائج" else "Retry loading the workspace results"
    scene.setContent {
        val s=state.value
        val g=generation.value
        MaterialTheme(colorScheme=theme) {
           CompositionLocalProvider(LocalLayoutDirection provides if(rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
              Box(Modifier.fillMaxSize().background(outside).padding(10.dp)) {
                 MadStatePanel(s,if(mode=="blank-title") " " else title(s),Modifier.width(width.dp),
                    message=if(mode=="blank-message") " " else message,
                    action=if(hasAction.value) MadStatePanelAction(if(mode=="blank-action") " " else action){calls+="${s.name}:$g"} else null)
              }
           }
        }
    }
    var time=0L
    fun frame(){repeat(5){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),time);time+=20_000_000L}}
    fun tap(p:Offset) {
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=time/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=time/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    fun checkState(at:MadPanelState,expectedAction:Boolean):SemanticsNode? {
        val o=owners.single()
        val raw=o.getAllSemanticsNodes(mergingEnabled=false)
        val merged=o.getAllSemanticsNodes(mergingEnabled=true)
        fun SemanticsNode.words()=config.getOrNull(SemanticsProperties.Text).orEmpty().map{it.text}
        val head=merged.single{it.config.contains(SemanticsProperties.Heading)}
        check(head.words()==listOf(title(at)) && raw.count { it.words()==listOf(title(at)) }==1)
        check(raw.count{it.words()==listOf(message)}==1)
        check((raw+merged).none{it.config.contains(SemanticsProperties.LiveRegion) || it.config.contains(SemanticsProperties.ContentDescription)})
        val progress=merged.filter{it.config.contains(SemanticsProperties.ProgressBarRangeInfo)}
        if(at==MadPanelState.Loading) check(progress.single().config[SemanticsProperties.ProgressBarRangeInfo]==ProgressBarRangeInfo.Indeterminate)
        else check(progress.isEmpty())
        val m=mutableListOf<TextLayoutResult>()
        check(head.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(m))
        check(m.single().lineCount>=1 && !m.single().hasVisualOverflow)
        check(m.single().getParagraphDirection(0)==if(rtl) ResolvedTextDirection.Rtl else ResolvedTextDirection.Ltr)
        val actions=merged.filter{it.config.contains(SemanticsActions.OnClick)}
        check(raw.count{it.config.contains(SemanticsActions.OnClick)}==if(expectedAction) 1 else 0)
        check(actions.size==if(expectedAction) 1 else 0)
        val button=actions.singleOrNull()
        if(button!=null) {
            check(button.words()==listOf(action) && button.config[SemanticsProperties.Role]==Role.Button)
            check(button.boundsInRoot.height>=48 && button.boundsInRoot.width>=48)
            val line=raw.single{it.words()==listOf(action)}
            val text=mutableListOf<TextLayoutResult>()
            check(line.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(text))
            val result=text.single()
            val last=result.lineCount-1
            // Skia paragraph metrics can exceed integer layout by a fractional pixel.
            check(!result.didOverflowWidth &&
                result.getLineEnd(last,visibleEnd=true)==action.length &&
                result.getLineBottom(last)<=result.size.height+1f &&
                !result.isLineEllipsized(last)) {
                "action clipped: ${result.size} lines=${result.lineCount} end=${result.getLineEnd(last,true)} bottom=${result.getLineBottom(last)}"
            }
            if(rtl) check(result.lineCount>1)
        }
        Bitmap().use { b ->
            b.allocN32Pixels(size.width,size.height);check(surface.readPixels(b,0,0))
            val left=if(rtl) size.width-10-width else 10
            val cols=(left+2 until left+width-2)
            val pixels=(12 until size.height-10).flatMap{y->cols.map{x->b.getColor(x,y)}}
            check(pixels.count{it==paper.toArgb()}>1000)
            if(at==MadPanelState.Error) check(pixels.count{it==error.toArgb()}>10)
            else check(pixels.none{it==error.toArgb()})
            if(at==MadPanelState.Loading) {
                val r=progress.single().boundsInRoot
                check((r.left.toInt() until r.right.toInt()).count {b.getColor(it,r.center.y.toInt())==primary.toArgb()} >20)
            }
        }
        return button
    }
    fun semantic(n:SemanticsNode){n.config[SemanticsActions.OnClick].action!!.invoke();frame()}
    try {
        frame();checkState(MadPanelState.Loading,false)
        state.value=MadPanelState.Empty;frame()
        val empty=checkNotNull(checkState(MadPanelState.Empty,true))
        tap(empty.boundsInRoot.center)
        check(calls==listOf("Empty:1") && state.value==MadPanelState.Empty)
        state.value=MadPanelState.Error;frame()
        val failed=checkNotNull(checkState(MadPanelState.Error,true))
        semantic(empty)
        check(calls==listOf("Empty:1")) { "old-state action dispatched into new state" }
        generation.value=2;frame()
        tap(failed.boundsInRoot.center)
        check(calls==listOf("Empty:1","Error:2") && state.value==MadPanelState.Error)
        state.value=MadPanelState.Loading;frame();checkState(MadPanelState.Loading,false)
        semantic(failed);check(calls.size==2) { "stale retry dispatched while loading" }
        state.value=MadPanelState.Empty;hasAction.value=false;frame();checkState(MadPanelState.Empty,false)
        semantic(empty);check(calls.size==2) { "removed action dispatched" }
        println("PASS panel RTL=$rtl width=$width scale=$scale: real Loading→Empty→Error→Loading→no-action, tight text + error/track pixels, host-only callbacks and no stale/live-region action")
    } finally {scene.close();surface.close()}
}
fun main(args:Array<String>){
    val result=runCatching{
        val mode=args.singleOrNull()
        require(mode in setOf(null,"blank-title","blank-message","blank-action"))
        probe(false,290,1f,mode)
        if(mode==null)probe(true,132,1.6f,null)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
