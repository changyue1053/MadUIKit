import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformTextInputMethodRequest
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadSearchField
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
    val submissions = mutableListOf<Pair<Int, TextFieldValue>>()
    val generation = mutableStateOf(1)
    val scene=androidx.compose.ui.scene.CanvasLayersComposeScene(density=Density(1f),size=IntSize(450,320),platformContext=host)
    val surface=Surface.makeRasterN32Premul(450,320)
    scene.setContent {
        Column {
            val callbackGeneration = generation.value
            MadSearchField(if(mode=="blank") "  " else "Search items",value.value,{
                changes++
                value.value=if(mode=="string-only") TextFieldValue(it.text) else it
            }, onSearch = { submissions.add(callbackGeneration to value.value) }, enabled=enabled.value,readOnly=readonly.value,supportingText=if (mode == "blank-help") " " else "Find an item by name",errorMessage=failure.value)

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
        check(field().config.getOrNull(SemanticsProperties.Text)?.map{it.text}==listOf("Search items","Find an item by name"))
        tap()
        check(field().config[SemanticsProperties.Focused])
        val input=checkNotNull(request){"No desktop platform input session"}
        check(input.imeOptions.imeAction==ImeAction.Search)
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
        check(submissions.isEmpty()) { "edits implicitly triggered search" }
        generation.value = 2
        render()
        checkNotNull(input.onImeAction).invoke(ImeAction.Search)
        render()
        check(submissions == listOf(2 to value.value)) { "search did not dispatch exactly once with current host state" }
        check(field().config[SemanticsProperties.Focused])
        check(value.value.selection == TextRange(1,3))
        check(owners.single().getAllSemanticsNodes(mergingEnabled=true).none {
            it.config.getOrNull(SemanticsProperties.Role) == Role.Button
        }) { "unexpected clear or submit button" }
        failure.value="Use 6 characters"
        render()
        check(field().config[SemanticsProperties.Error]=="Use 6 characters")
        check(owners.single().getAllSemanticsNodes(mergingEnabled=true)
            .filter { it.config.contains(SemanticsProperties.Error) }
            .map { it.config[SemanticsProperties.Error] } == listOf("Use 6 characters"))
        check(field().config.getOrNull(SemanticsProperties.Text)?.map{it.text}==listOf("Search items","Use 6 characters"))
        val before=value.value.text
        readonly.value=true;render()
        check(value.value.text==before)
        tap()
        check(field().config[SemanticsProperties.Focused])
        check(request==null)
        checkNotNull(input.onImeAction).invoke(ImeAction.Search)
        render()
        check(submissions.size == 1) { "stale search action submitted while read-only" }
        check(!field().config.contains(SemanticsActions.SetText))
        val changeCount=changes
        enabled.value=false;render();tap()
        check(field().config.contains(SemanticsProperties.Disabled) && field().config.getOrNull(SemanticsProperties.Focused)!=true)
        check(request==null && value.value.text==before && changes==changeCount)
        checkNotNull(input.onImeAction).invoke(ImeAction.Search)
        render()
        check(submissions.size == 1) { "stale search action submitted while disabled" }
        println("PASS headless Compose platform-session edit commands: CJK composition update, commit, caret/selection, Search once with latest callback, no implicit submission/clear, support/error, read-only/disabled stale action suppression")
    } finally { scene.close();surface.close() }
}
