import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.*
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun probe(mode:String?) {
    val owners=mutableSetOf<SemanticsOwner>()
    var activeInput:PlatformTextInputMethodRequest?=null
    val host=object:PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener=object:PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner:SemanticsOwner){owners.add(semanticsOwner)}
            override fun onSemanticsOwnerRemoved(semanticsOwner:SemanticsOwner){owners.remove(semanticsOwner)}
            override fun onSemanticsChange(semanticsOwner:SemanticsOwner){}
            override fun onLayoutChange(semanticsOwner:SemanticsOwner,semanticsNodeId:Int){}
        }
        override suspend fun startInputMethod(request:PlatformTextInputMethodRequest):Nothing {
            try{activeInput=request;awaitCancellation()}finally{if(activeInput===request) activeInput=null}
        }
    }
    val size=IntSize(360,280)
    val scene=CanvasLayersComposeScene(density=Density(1f),size=size,platformContext=host)
    val surface=Surface.makeRasterN32Premul(size.width,size.height)
    val ordinary=Color(0xff204241)
    val error=Color(0xff9e0436)
    val value=mutableStateOf(TextFieldValue("e\u0301",TextRange(2)))
    val alternate=mutableStateOf(false)
    val limit=mutableStateOf(6)
    fun visible(n:Int,k:Int)=if(n>k) "$n / $k bytes — over limit" else "$n / $k bytes"
    fun spoken(n:Int,k:Int)=if(n>k) "$n bytes used; exceeds the limit of $k bytes" else "$n of $k bytes used"
    scene.setContent {
        MaterialTheme(colorScheme=lightColorScheme(onSurfaceVariant=ordinary,error=error)){
            Column(Modifier.fillMaxSize().background(Color.White).padding(18.dp)){
                MadTextArea("Project notes",value.value,{value.value=if(mode=="truncate-overflow" && it.text.encodeToByteArray().size>6) value.value else it},
                    modifier=Modifier.fillMaxWidth(),supportingText="Changes are not sent yet")
                Spacer(Modifier.height(12.dp))
                val n=if(mode=="negative-current") -1 else value.value.text.encodeToByteArray().size
                val k=if(mode=="negative-limit") -1 else limit.value
                MadFieldLength(n,k,if(mode=="blank-message") " " else visible(n,k),
                    spokenMessage=if(mode=="blank-spoken") " " else if(alternate.value) spoken(n,k) else null)
            }
        }
    }
    var t=0L
    fun frame(){repeat(6){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),t);t+=20_000_000L}}
    fun nodes(merged:Boolean=true)=owners.single().getAllSemanticsNodes(mergingEnabled=merged)
    fun SemanticsNode.words()=config.getOrNull(SemanticsProperties.Text).orEmpty().map{it.text}
    fun inspect(n:Int,k:Int){
        val expected=if(alternate.value) spoken(n,k) else visible(n,k)
        val merged=nodes()
        val cue=merged.single{it.words()==listOf(expected) ||
            it.config.getOrNull(SemanticsProperties.ContentDescription)==listOf(expected)}
        val raw=nodes(false)
        check((raw+merged).none{it.config.contains(SemanticsProperties.LiveRegion)})
        check(!cue.config.contains(SemanticsActions.OnClick))
        check(merged.count{it.config.contains(SemanticsProperties.EditableText)}==1)
        if(alternate.value) {
            check(raw.count{it.config.getOrNull(SemanticsProperties.ContentDescription)==listOf(expected)}==1)
            check(merged.none{visible(n,k) in it.words()})
        }
        val r=cue.boundsInRoot
        Bitmap().use{b->
            b.allocN32Pixels(size.width,size.height);check(surface.readPixels(b,0,0))
            val p=(r.top.toInt() until r.bottom.toInt()).flatMap{y->(r.left.toInt() until r.right.toInt()).map{x->b.getColor(x,y)}}
            fun red(argb:Int)=(argb ushr 16) and 255
            fun green(argb:Int)=(argb ushr 8) and 255
            val tealInk=p.count{green(it)-red(it)>5}
            val redInk=p.count{red(it)-green(it)>24}
            check(if(n>k) redInk>12 && tealInk==0 else tealInk>12 && redInk==0) {
                "wrong painted counter tone: teal=$tealInk red=$redInk"
            }
        }
    }
    try{
        frame();inspect(3,6)
        val e=nodes().single{it.config.contains(SemanticsProperties.EditableText)}
        val pos=e.boundsInRoot.center
        scene.sendPointerEvent(PointerEventType.Press,pos,timeMillis=t/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,pos,timeMillis=t/1_000_000+1,type=PointerType.Touch)
        frame()
        val input=checkNotNull(activeInput)
        input.onEditCommand(listOf(SetSelectionCommand(2,2),SetComposingTextCommand("你",1)))
        frame()
        check(value.value.text=="e\u0301你" && value.value.selection==TextRange(3) &&
            value.value.composition==TextRange(2,3))
        inspect(6,6) // equality is normal.
        input.onEditCommand(listOf(SetComposingTextCommand("你好",1)))
        frame()
        check(value.value.text=="e\u0301你好" && value.value.selection==TextRange(4) &&
            value.value.composition==TextRange(2,4) && activeInput===input) { "overflow discarded composing text or caret" }
        inspect(9,6)
        alternate.value=true;frame();inspect(9,6)
        input.onEditCommand(listOf(FinishComposingTextCommand(),SetSelectionCommand(2,4)))
        frame()
        check(value.value.text=="e\u0301你好" && value.value.selection==TextRange(2,4) && value.value.composition==null)
        inspect(9,6)
        input.onEditCommand(listOf(SetSelectionCommand(0,4),CommitTextCommand("",1)))
        limit.value=0;frame();inspect(0,0)
        check(value.value==TextFieldValue("") && activeInput===input)
        println("PASS exact host UTF-8 boundaries 3/6→6/6→9/6→0/0 with combining+CJK preservation, one accessible status, no live region, actual normal/overflow ink")
    } finally { scene.close();surface.close() }
}

fun main(args:Array<String>){
    val mode=args.singleOrNull()
    val result=runCatching{
        require(mode in setOf(null,"negative-current","negative-limit","blank-message","blank-spoken","truncate-overflow"))
        probe(mode)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
