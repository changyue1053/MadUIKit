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
    val size=IntSize(380,510)
    val scene=CanvasLayersComposeScene(density=Density(1f,scale),size=size,platformContext=host)
    val surface=Surface.makeRasterN32Premul(size.width,size.height)
    val tone=mutableStateOf(MadBannerTone.Neutral)
    val identity=mutableStateOf(1)
    val generation=mutableStateOf(1)
    val visible=mutableStateOf(true)
    val action=mutableStateOf(false)
    val calls=mutableListOf<String>()
    val paper=Color(if(rtl) 0xff172b32 else 0xfff5faf6)
    val accent=mapOf(
        MadBannerTone.Neutral to Color(if(rtl) 0xff99d7b0 else 0xff075e43),
        MadBannerTone.Warning to Color(if(rtl) 0xffffd086 else 0xff8e5312),
        MadBannerTone.Error to Color(if(rtl) 0xffffbcb5 else 0xffa00224),
    )
    val outside=Color(0xffce639c)
    val theme=if(rtl) darkColorScheme(surfaceContainer=paper,error=accent.getValue(MadBannerTone.Error),
        primary=accent.getValue(MadBannerTone.Neutral),tertiary=accent.getValue(MadBannerTone.Warning))
       else lightColorScheme(surfaceContainer=paper,error=accent.getValue(MadBannerTone.Error),
        primary=accent.getValue(MadBannerTone.Neutral),tertiary=accent.getValue(MadBannerTone.Warning))
    fun status(at:MadBannerTone)=when(at) {
        MadBannerTone.Neutral -> if(rtl) "إشعار" else "Notice"
        MadBannerTone.Warning -> if(rtl) "تحذير" else "Warning"
        MadBannerTone.Error -> if(rtl) "خطأ" else "Error"
    }
    fun message(id:Int)=if(rtl) "هذه الرسالة حول حالة المجلد المحلي رقم $id" else "Local folder $id will remain available for review."
    val label=if(rtl) "إغلاق هذه الرسالة الحالية" else "Dismiss this folder notice"
    scene.setContent {
        val s=tone.value; val num=identity.value; val g=generation.value
        MaterialTheme(colorScheme=theme) {
           CompositionLocalProvider(LocalLayoutDirection provides if(rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
              Box(Modifier.fillMaxSize().background(outside).padding(10.dp)) {
                 Column(Modifier.width(width.dp)) {
                     if(visible.value) MadBanner(s,if(mode=="blank-status") " " else status(s),
                        if(mode=="blank-message") " " else message(num),
                        action=if(action.value || mode=="blank-action") MadBannerAction(if(mode=="blank-action") " " else label){
                            calls+="${s.name}:$num:$g"
                        } else null)
                     androidx.compose.material3.Text("Workspace")
                 }
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
    fun checkState(at:MadBannerTone,id:Int,expectedAction:Boolean):SemanticsNode? {
        val o=owners.single()
        val raw=o.getAllSemanticsNodes(mergingEnabled=false)
        val merged=o.getAllSemanticsNodes(mergingEnabled=true)
        fun SemanticsNode.words()=config.getOrNull(SemanticsProperties.Text).orEmpty().map{it.text}
        check(raw.count{it.words()==listOf(status(at))}==1)
        val body=raw.single{it.words()==listOf(message(id))}
        check(merged.count{it.words()==listOf("Workspace")}==1)
        check((raw+merged).none{it.config.contains(SemanticsProperties.LiveRegion) ||
            it.config.contains(SemanticsProperties.ContentDescription) ||
            it.config.contains(SemanticsProperties.ProgressBarRangeInfo)})
        fun layout(n:SemanticsNode,s:String):TextLayoutResult {
            val l=mutableListOf<TextLayoutResult>()
            check(n.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(l))
            val m=l.single(); val last=m.lineCount-1
            check(m.getLineEnd(last,true)==s.length && m.getLineBottom(last)<=m.size.height+1f && !m.isLineEllipsized(last))
            return m
        }
        val bodyLayout=layout(body,message(id))
        if(rtl) check(bodyLayout.lineCount>1)
        val actions=merged.filter{it.config.contains(SemanticsActions.OnClick)}
        check(raw.count{it.config.contains(SemanticsActions.OnClick)}==if(expectedAction) 1 else 0)
        check(actions.size==if(expectedAction) 1 else 0)
        val button=actions.singleOrNull()
        if(button!=null) {
            check(button.words()==listOf(label) && button.config[SemanticsProperties.Role]==Role.Button)
            check(button.boundsInRoot.height>=48 && button.boundsInRoot.width>=48)
            val buttonLayout=layout(raw.single{it.words()==listOf(label)},label)
            if(rtl) check(buttonLayout.lineCount>1)
        }
        Bitmap().use { b ->
            b.allocN32Pixels(size.width,size.height);check(surface.readPixels(b,0,0))
            val left=if(rtl) size.width-10-width else 10
            val dir=if(rtl) -1 else 1
            val start=if(rtl) left+width-1 else left
            val opposite=if(rtl) left else left+width-1
            check(b.getColor(start,25)==accent.getValue(at).toArgb())
            check(b.getColor(start+dir,25)==accent.getValue(at).toArgb())
            check(b.getColor(start+dir*2,25)==paper.toArgb())
            check(b.getColor(start-dir,25)==outside.toArgb())
            check(b.getColor(opposite,25)==paper.toArgb())
        }
        return button
    }
    fun semantic(n:SemanticsNode){n.config[SemanticsActions.OnClick].action!!.invoke();frame()}
    try {
        frame();checkState(MadBannerTone.Neutral,1,false)
        tone.value=MadBannerTone.Warning;action.value=true;frame()
        val warn=checkNotNull(checkState(MadBannerTone.Warning,1,true))
        tap(warn.boundsInRoot.center)
        check(calls==listOf("Warning:1:1"))
        checkState(MadBannerTone.Warning,1,true) // no automatic dismissal
        identity.value=2;tone.value=MadBannerTone.Error;frame()
        val error=checkNotNull(checkState(MadBannerTone.Error,2,true))
        semantic(warn);check(calls==listOf("Warning:1:1")) { "replaced notice called the new action" }
        generation.value=2;frame()
        tap(error.boundsInRoot.center);check(calls==listOf("Warning:1:1","Error:2:2"))
        time+=4_000_000_000L;frame();checkState(MadBannerTone.Error,2,true)
        visible.value=false;frame()
        val merged=owners.single().getAllSemanticsNodes(mergingEnabled=true)
        check(merged.none{it.config.contains(SemanticsActions.OnClick)})
        semantic(error);check(calls.size==2) { "removed notice dispatched stale action" }
        println("PASS banner RTL=$rtl width=$width scale=$scale: three text-labeled tones and actual mirrored stripe, long text/action fit, host-only persistence/replacement/removal, stale actions zero")
    } finally {scene.close();surface.close()}
}
fun main(args:Array<String>){
    val result=runCatching{
        val mode=args.singleOrNull()
        require(mode in setOf(null,"blank-status","blank-message","blank-action"))
        probe(false,290,1f,mode)
        if(mode==null)probe(true,128,1.6f,null)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
