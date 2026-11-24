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
import com.mad.maduikit.MadTextField
import kotlinx.coroutines.awaitCancellation
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    val mode = args.singleOrNull()
    require(mode in setOf(null, "blank", "blank-help", "blank-error", "string-only"))
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
    var changes=0
    var done=0
    val scene=androidx.compose.ui.scene.CanvasLayersComposeScene(density=Density(1f),size=IntSize(450,320),platformContext=host)
    val surface=Surface.makeRasterN32Premul(450,320)
    scene.setContent {
        Column {
            MadTextField(if(mode=="blank") "  " else "Display name",value.value,{
                changes++
                value.value=if(mode=="string-only") TextFieldValue(it.text) else it
            }, enabled=enabled.value,readOnly=readonly.value,supportingText=if (mode == "blank-help") " " else "Use a public name",errorMessage=failure.value,
            keyboardOptions=KeyboardOptions(imeAction=ImeAction.Done), keyboardActions=KeyboardActions(onDone={done++}))
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
        check(field().config.getOrNull(SemanticsProperties.Text)?.map{it.text}==listOf("Display name","Use a public name"))
        tap()
        check(field().config[SemanticsProperties.Focused])
        val input=checkNotNull(request){"No desktop platform input session"}
        check(input.imeOptions.imeAction==ImeAction.Done)
        input.onEditCommand(listOf(SetSelectionCommand(1,1),SetComposingTextCommand("你",1)))
        render()
        check(value.value.text=="a你b" && value.value.selection==TextRange(2) && value.value.composition==TextRange(1,2)) {
            "composition/caret lost after forwarding"
        }
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
        if (mode == null) nextDone()
        println("PASS headless Compose platform-session edit commands: CJK composition update, commit, caret/selection, Done, support/error, read-only/disabled")
    } finally { scene.close();surface.close() }
}

@OptIn(InternalComposeUiApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun nextDone() {
    var active: PlatformTextInputMethodRequest? = null
    var next = 0
    var done = 0
    val host = object : PlatformContext by PlatformContext.Empty {
        override suspend fun startInputMethod(request: PlatformTextInputMethodRequest): Nothing {
            try {
                active = request
                awaitCancellation()
            } finally {
                if (active === request) active = null
            }
        }
    }
    val first = androidx.compose.ui.focus.FocusRequester()
    val second = androidx.compose.ui.focus.FocusRequester()
    val focus = arrayOf(false, false)
    val field1 = mutableStateOf(TextFieldValue("First"))
    val field2 = mutableStateOf(TextFieldValue("Second"))
    val scene = androidx.compose.ui.scene.CanvasLayersComposeScene(
        density = Density(1f), size = IntSize(450,320), platformContext = host,
    )
    val surface = Surface.makeRasterN32Premul(450,320)
    scene.setContent {
        val manager = androidx.compose.ui.platform.LocalFocusManager.current
        Column {
            MadTextField("First field", field1.value, { field1.value = it },
                modifier = Modifier.focusRequester(first).onFocusChanged { focus[0] = it.isFocused },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { next++; second.requestFocus() }),
            )
            MadTextField("Second field", field2.value, { field2.value = it },
                modifier = Modifier.focusRequester(second).onFocusChanged { focus[1] = it.isFocused },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { done++; manager.clearFocus() }),
            )
        }
    }
    var t = 0L
    fun render() { repeat(8) { surface.canvas.clear(0); scene.render(surface.canvas.asComposeCanvas(),t); t+=20_000_000 } }
    try {
        render()
        first.requestFocus()
        render()
        check(focus.contentEquals(arrayOf(true,false)))
        val a = checkNotNull(active)
        check(a.imeOptions.imeAction == ImeAction.Next)
        checkNotNull(a.onImeAction).invoke(ImeAction.Next)
        render()
        check(next == 1 && done == 0 && focus.contentEquals(arrayOf(false,true)))
        val b = checkNotNull(active)
        check(b !== a && b.imeOptions.imeAction == ImeAction.Done)
        checkNotNull(b.onImeAction).invoke(ImeAction.Done)
        render()
        check(next == 1 && done == 1 && focus.contentEquals(arrayOf(false,false)) && active == null)
        check(field1.value.text == "First" && field2.value.text == "Second")
        println("PASS two-field Desktop focus contract: host Next chooses second, Done once clears focus")
    } finally { scene.close(); surface.close() }
}
