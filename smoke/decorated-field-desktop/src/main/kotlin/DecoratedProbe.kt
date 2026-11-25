import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadDecoratedTextField
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.skia.Bitmap
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val mode = args.singleOrNull()
    require(mode in setOf(null, "blank-label", "blank-help", "blank-error", "string-only", "blank-placeholder"))
    val owners=mutableSetOf<SemanticsOwner>()
    var request: PlatformTextInputMethodRequest?=null
    val host=object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener=object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner:SemanticsOwner) {owners.add(semanticsOwner)}
            override fun onSemanticsOwnerRemoved(semanticsOwner:SemanticsOwner) {owners.remove(semanticsOwner)}
            override fun onSemanticsChange(semanticsOwner:SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner:SemanticsOwner,semanticsNodeId:Int) {}
        }
        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            // Exact Compose platform boundary, not an actual system keyboard.
            try { latest(request); awaitCancellation() } finally { latest(null) }
        }
        private fun latest(v: PlatformTextInputMethodRequest?) { request=v }
    }
    val value=mutableStateOf(TextFieldValue("ab",TextRange(1)))
    val readonly=mutableStateOf(false)
    val enabled=mutableStateOf(true)
    val failure=mutableStateOf<String?>(if (mode == "blank-error") " " else null)
    val showExample=mutableStateOf(true)
    val width=mutableStateOf(226)
    fun art(c:Color)=object:Painter(){
        override val intrinsicSize=Size(72f,40f)
        override fun DrawScope.onDraw(){drawRect(c,Offset(-30f,-30f),Size(84f,84f))}
    }
    val a=Color(0xffda4777); val b=Color(0xff198f54)
    val leading=art(a); val trailing=art(b)
    var changes=0
    var done=0
    val scene=androidx.compose.ui.scene.CanvasLayersComposeScene(density=Density(1f,1.6f),size=IntSize(450,320),platformContext=host)
    val surface=Surface.makeRasterN32Premul(450,320)
    scene.setContent {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
          Column {
            MadDecoratedTextField(if(mode=="blank-label") "  " else "Display name",value.value,{
                changes++
                value.value=if(mode=="string-only") TextFieldValue(it.text) else it
            }, modifier=Modifier.width(width.value.dp), placeholder=if(mode=="blank-placeholder") " " else if(showExample.value) "Example only" else null, leading=leading,trailing=trailing, enabled=enabled.value,readOnly=readonly.value,supportingText=if (mode == "blank-help") " " else "Use a public name",errorMessage=failure.value,
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done), keyboardActions=KeyboardActions(onDone={done++}))
          }
        }
    }
    var time=0L
    fun render() {repeat(8){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),time);time+=20_000_000}}
    fun field()=owners.single().getAllSemanticsNodes(mergingEnabled=true).single{it.config.contains(SemanticsProperties.EditableText)}
    fun tap(p:Offset=field().boundsInRoot.center) {
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=time/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=time/1_000_000+1,type=PointerType.Touch)
        render()
    }
    try {
        render()
        check(field().config.getOrNull(SemanticsProperties.Text)?.map{it.text}==listOf("Display name","Use a public name"))
        fun colors():Pair<List<Int>,List<Int>> {
            val o=owners.single()
            val raw=o.getAllSemanticsNodes(mergingEnabled=false)
            check(raw.none{it.config.contains(SemanticsActions.OnClick) && !it.config.contains(SemanticsProperties.EditableText)})
            check(raw.none { it.config.contains(SemanticsProperties.ContentDescription) ||
                it.config.getOrNull(SemanticsProperties.Role)==Role.Image })
            Bitmap().use { buf ->
                buf.allocN32Pixels(450,320);check(surface.readPixels(buf,0,0))
                val aa=mutableListOf<Int>();val bb=mutableListOf<Int>()
                for(y in 0 until 320) for(x in 0 until 450) when(buf.getColor(x,y)){
                    a.toArgb()->aa.add(y*450+x)
                    b.toArgb()->bb.add(y*450+x)
                }
                return aa to bb
            }
        }
        val (start,end)=colors()
        check(start.size==576 && end.size==576 && start.first()%450>end.first()%450) { "oversized art was not clipped or mirrored: ${start.size}/${end.size}" }
        val pixel=start[start.size/2+12]
        tap(Offset((pixel%450).toFloat(),(pixel/450).toFloat()))
        check(value.value.text=="ab" && done==0)
        check(field().config[SemanticsProperties.Focused])
        val input=checkNotNull(request){"No desktop platform input session"}
        check(input.imeOptions.imeAction==ImeAction.Done)
        if(mode==null) {
            value.value=TextFieldValue("");render()
            fun sample():List<Int> {
                val r=field().boundsInRoot
                Bitmap().use { b ->
                    b.allocN32Pixels(450,320);check(surface.readPixels(b,0,0))
                    return (r.top.toInt()+24 until r.top.toInt()+68).flatMap { y->
                       (r.left.toInt()+55 until r.right.toInt()-55).map { x->b.getColor(x,y) }
                    }
                }
            }
            val with=sample()
            showExample.value=false;render()
            check(with.zip(sample()).count{(u,v)->u!=v}>20) { "focused empty placeholder not painted" }
            check(field().config[SemanticsProperties.Text].map{it.text}==listOf("Display name","Use a public name"))
            showExample.value=true
            value.value=TextFieldValue("ab",TextRange(1));render()
        }

        input.onEditCommand(listOf(SetSelectionCommand(1,1),SetComposingTextCommand("你",1)))
        render()
        check(value.value.text=="a你b" && value.value.selection==TextRange(2) && value.value.composition==TextRange(1,2)) {
            "composition/caret lost after forwarding"
        }
        width.value=168;render()
        check(colors().let { it.first.size==576 && it.second.isEmpty() })
        width.value=112;render()
        check(colors().let { it.first.isEmpty() && it.second.isEmpty() })
        check(value.value.composition==TextRange(1,2) && request===input)
        input.onEditCommand(listOf(SetComposingTextCommand("你好",1)))
        render()
        check(value.value.text=="a你好b" && value.value.selection==TextRange(3) && value.value.composition==TextRange(1,3))
        input.onEditCommand(listOf(FinishComposingTextCommand(),SetSelectionCommand(1,3)))
        render()
        check(value.value.text=="a你好b" && value.value.selection==TextRange(1,3) && value.value.composition==null)
        checkNotNull(input.onImeAction).invoke(ImeAction.Done); render(); check(done==1)
        failure.value="Use 6 characters"
        render()
        check(field().config[SemanticsProperties.Error]=="Use 6 characters")
        check(owners.single().getAllSemanticsNodes(mergingEnabled=true)
            .filter { it.config.contains(SemanticsProperties.Error) }
            .map { it.config[SemanticsProperties.Error] } == listOf("Use 6 characters"))
        check(field().config.getOrNull(SemanticsProperties.Text)?.map{it.text}==listOf("Display name","Use 6 characters"))
        val before=value.value.text
        readonly.value=true;render()
        check(value.value.text==before)
        tap()
        check(field().config[SemanticsProperties.Focused])
        check(request==null)
        check(!field().config.contains(SemanticsActions.SetText))
        val changeCount=changes
        enabled.value=false;render();tap()
        check(field().config.contains(SemanticsProperties.Disabled) && field().config.getOrNull(SemanticsProperties.Focused)!=true)
        check(request==null && value.value.text==before && done==1 && changes==changeCount)
        println("PASS RTL 1.6 with 226→168→112dp, bounded/mirrored passive Painter clipping, no duplicate names or actions; real Compose edit commands: CJK composition update, commit, caret/selection, Done, support/error, read-only/disabled")
    } finally { scene.close();surface.close() }
}
