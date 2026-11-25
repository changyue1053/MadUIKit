import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.width
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadTextArea
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun probe(mode: String?, rtl:Boolean) {
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
    val value=mutableStateOf(TextFieldValue("",TextRange(0)))
    val exampleVisible=mutableStateOf(true)
    val readonly=mutableStateOf(false)
    val enabled=mutableStateOf(true)
    val failure=mutableStateOf<String?>(null)
    val width=if(rtl) 132 else 300
    val label=if(rtl) "ملاحظات المشروع" else "Project notes"
    val help=if(rtl) "ملخص ملاحظات العمل" else "Use complete sentences"
    val error=if(rtl) "أضف خطوة تالية" else "Add a next step"
    val done=mutableListOf<String>()
    val scene=androidx.compose.ui.scene.CanvasLayersComposeScene(density=Density(1f,if(rtl)1.6f else 1f),size=IntSize(450,620),platformContext=host)
    val surface=Surface.makeRasterN32Premul(450,620)
    scene.setContent {
        CompositionLocalProvider(LocalLayoutDirection provides if(rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
            Column {
                MadTextArea(if(mode=="blank-label") " " else label,value.value,{
                    value.value=if(mode=="string-only") TextFieldValue(it.text) else it
                }, modifier=Modifier.width(width.dp), minVisibleLines=if(mode=="range") 5 else 2,
                    maxVisibleLines=4, placeholder=if(mode=="blank-placeholder") " " else if(exampleVisible.value) "Example text" else null,
                    enabled=enabled.value,readOnly=readonly.value,
                    supportingText=if(mode=="blank-help") " " else help,
                    errorMessage=if(mode=="blank-error") " " else failure.value,
                    keyboardOptions=if(mode=="done") KeyboardOptions(imeAction=ImeAction.Done) else KeyboardOptions.Default,
                    keyboardActions=KeyboardActions(onDone={done.add(value.value.text)}))
            }
        }
    }
    var time=0L
    fun render() {repeat(8){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),time);time+=20_000_000}}
    fun field()=owners.single().getAllSemanticsNodes(mergingEnabled=true).single{it.config.contains(SemanticsProperties.EditableText)}
    fun tap() {
        val p=field().boundsInRoot.center
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=time/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=time/1_000_000+1,type=PointerType.Touch)
        render()
    }
    try {
        render()
        check(field().config.getOrNull(SemanticsProperties.Text)?.map{it.text}==listOf(label,help))
        val base=field().boundsInRoot.height
        tap()
        check(field().config[SemanticsProperties.Focused])
        check(field().config.getOrNull(SemanticsProperties.Text)?.map{it.text}==listOf(label,help))
        val input=checkNotNull(request)
        check(!input.imeOptions.singleLine)
        if(mode=="done") {
            check(input.imeOptions.imeAction==ImeAction.Done)
            checkNotNull(input.onImeAction).invoke(ImeAction.Done);render()
            check(done==listOf("") && value.value.text.isEmpty())
            println("PASS explicit host Done configuration")
            return
        }
        if(!rtl && mode==null) {
            fun inputPixels():List<Int> {
                val r=field().boundsInRoot
                Bitmap().use { b ->
                    b.allocN32Pixels(450,620);check(surface.readPixels(b,0,0))
                    return (r.top.toInt()+20 until r.top.toInt()+55).flatMap { y ->
                        (r.left.toInt()+24 until r.right.toInt()-12).map { x -> b.getColor(x,y) }
                    }
                }
            }
            val shown=inputPixels()
            exampleVisible.value=false;render()
            check(field().config[SemanticsProperties.Text].map{it.text}==listOf(label,help))
            check(shown.zip(inputPixels()).count{(a,b)->a!=b}>20) { "visual placeholder produced no raster content" }
            exampleVisible.value=true;render()
            scene.sendKeyEvent(KeyEvent(Key.Enter,KeyEventType.KeyDown))
            scene.sendKeyEvent(KeyEvent(Key.Enter,KeyEventType.KeyUp));render()
            check(value.value.text=="\n" && done.isEmpty()) { "Desktop Return did not insert one newline" }
            input.onEditCommand(listOf(SetSelectionCommand(0,1),CommitTextCommand("",1)))
            render();check(value.value.text.isEmpty())
        }
        input.onEditCommand(listOf(CommitTextCommand("a\nb",1),SetSelectionCommand(2,2),SetComposingTextCommand("你",1)))
        render()
        check(value.value.text=="a\n你b" && value.value.selection==TextRange(3) && value.value.composition==TextRange(2,3)) {
            "multiline composing/caret lost"
        }
        input.onEditCommand(listOf(SetComposingTextCommand("你好",1),FinishComposingTextCommand(),SetSelectionCommand(2,4)))
        render()
        check(value.value.text=="a\n你好b" && value.value.selection==TextRange(2,4) && value.value.composition==null)
        input.onEditCommand(listOf(SetSelectionCommand(5,5),CommitTextCommand("\nc\nd\ne\nf",1)))
        render()
        check(value.value.text.lines().size==6)
        val maximum=field().boundsInRoot.height
        check(maximum>base)
        input.onEditCommand(listOf(CommitTextCommand("\ng\nh",1)))
        render()
        check(value.value.text.lines().size==8 && field().boundsInRoot.height==maximum)
        check(value.value.text=="a\n你好b\nc\nd\ne\nf\ng\nh")
        failure.value=error;render()
        val m=owners.single().getAllSemanticsNodes(mergingEnabled=true)
        check(field().config[SemanticsProperties.Text].map{it.text}==listOf(label,error))
        check(m.filter{it.config.contains(SemanticsProperties.Error)}.map{it.config[SemanticsProperties.Error]}==listOf(error))
        val before=value.value
        readonly.value=true;render();tap()
        check(field().config[SemanticsProperties.Focused] && request==null &&
            !field().config.contains(SemanticsActions.SetText))
        enabled.value=false;render();tap()
        check(field().config.contains(SemanticsProperties.Disabled) &&
            field().config.getOrNull(SemanticsProperties.Focused)!=true && request==null)
        check(before.text==value.value.text && done.isEmpty())
        println("PASS TextArea RTL=$rtl width=$width: visible lines $base to $maximum then capped; original 8 lines, CJK composition/caret, label/help→error, read-only/disabled, no implicit submit")
    } finally { scene.close();surface.close() }
}


fun main(args:Array<String>) {
    val mode=args.singleOrNull()
    val result=runCatching {
        require(mode in setOf(null,"done","range","string-only","blank-label","blank-placeholder","blank-help","blank-error"))
        probe(mode,false)
        if(mode==null) probe(null,true)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
