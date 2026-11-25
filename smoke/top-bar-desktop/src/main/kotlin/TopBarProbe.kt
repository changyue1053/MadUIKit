import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.painter.ColorPainter
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
private fun probe(rtl:Boolean, scale:Float, width:Int, count:Int, mode:String?) {
    val owners=mutableSetOf<SemanticsOwner>()
    val host=object:PlatformContext by PlatformContext.Empty{
        override val semanticsOwnerListener=object:PlatformContext.SemanticsOwnerListener{
            override fun onSemanticsOwnerAppended(semanticsOwner:SemanticsOwner){owners.add(semanticsOwner)}
            override fun onSemanticsOwnerRemoved(semanticsOwner:SemanticsOwner){owners.remove(semanticsOwner)}
            override fun onSemanticsChange(semanticsOwner:SemanticsOwner){}
            override fun onLayoutChange(semanticsOwner:SemanticsOwner,semanticsNodeId:Int){}
        }
    }
    val scene=CanvasLayersComposeScene(density=Density(1f,scale),size=IntSize(390,235),platformContext=host)
    val surface=Surface.makeRasterN32Premul(390,235)
    val outside=Color(0xffb43a82)
    val fill=Color(if(rtl) 0xff162b29 else 0xffeffaf2)
    val ink=Color(if(rtl) 0xffe5ffea else 0xff153f31)
    val theme=if(rtl) darkColorScheme(surfaceContainer=fill,onSurface=ink)
        else lightColorScheme(surfaceContainer=fill,onSurface=ink)
    val title=if(rtl) "مساحة العمل المشتركة ومراجعة نتائج البحث السابقة وتفاصيل الأعضاء والمحتويات والمهام"
        else "Review workspace materials and saved collections from this long project heading"
    val start=if(rtl) "رجوع" else "Go back"
    val end=if(rtl) "خيارات المشروع" else "Project options"
    val calls=mutableListOf<String>()
    val icon=ColorPainter(Color.Cyan)
    scene.setContent{
        MaterialTheme(colorScheme=theme) {
            CompositionLocalProvider(LocalLayoutDirection provides if(rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize().background(outside).padding(10.dp)) {
                    MadTopAppBar(if(mode=="blank-title") " " else title, Modifier.width(width.dp),
                        navigation=if(count==2) MadTopAppBarAction(icon,if(mode=="blank-action") " " else start){ calls+="start" } else null,
                        action=if(count>0) MadTopAppBarAction(icon,if(mode=="duplicate") " $start " else end){calls+="end"} else null)
                }
            }
        }
    }
    var t=0L
    fun frame(){ repeat(4){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),t);t+=20_000_000L} }
    fun tap(p:Offset){
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=t/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=t/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    try {
        frame()
        val o=owners.single()
        val nodes=o.getAllSemanticsNodes(mergingEnabled=true)
        val raw=o.getAllSemanticsNodes(mergingEnabled=false)
        val text=nodes.single{it.config.contains(SemanticsProperties.Heading)}
        check(text.config[SemanticsProperties.Text].map{it.text}==listOf(title))
        val layouts=mutableListOf<TextLayoutResult>()
        check(text.config[SemanticsActions.GetTextLayoutResult].action!!.invoke(layouts))
        val layout=layouts.single()
        check(layout.lineCount==2 && layout.hasVisualOverflow && layout.getLineEnd(1,visibleEnd=true)<title.length)
        check(layout.getParagraphDirection(0)==if(rtl) ResolvedTextDirection.Rtl else ResolvedTextDirection.Ltr)
        val actions=nodes.filter{it.config.contains(SemanticsActions.OnClick)}.associateBy {
            check(it.config[SemanticsProperties.Role]==Role.Button)
            it.config[SemanticsProperties.ContentDescription].single()
        }
        check(actions.keys==when(count){0->emptySet();1->setOf(end);else->setOf(start,end)})
        check(raw.count{it.config.contains(SemanticsActions.OnClick)}==count)
        check(raw.sumOf{it.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().size}==count)
        val left=if(rtl) 390-10-width else 10
        val right=left+width
        if (count>0) {
            val a=actions.getValue(end).boundsInRoot
            check(a.width==48f && a.height==48f && if(rtl) a.left==left.toFloat() else a.right==right.toFloat())
        }
        if(count==2) {
            val a=actions.getValue(start).boundsInRoot
            val b=actions.getValue(end).boundsInRoot
            check(a.width==48f && a.height==48f && if(rtl) a.right==right.toFloat() else a.left==left.toFloat())
            if(width<96) check(a.bottom<=b.top)
            if(width<160) check(text.boundsInRoot.bottom<=a.top && text.boundsInRoot.bottom<=b.top)
            else check(if(rtl) text.boundsInRoot.right<=a.left && text.boundsInRoot.left>=b.right
                else text.boundsInRoot.left>=a.right && text.boundsInRoot.right<=b.left)
        }
        Bitmap().use { b ->
            b.allocN32Pixels(390,235);check(surface.readPixels(b,0,0))
            check(b.getColor(left,10)==fill.toArgb() && b.getColor(right,10)==outside.toArgb())
            actions.values.forEach { a ->
                val c=a.boundsInRoot.center
                check(b.getColor(c.x.toInt(),c.y.toInt())==ink.toArgb())
            }
        }
        tap(text.boundsInRoot.center)
        check(calls.isEmpty())
        for ((label,callback) in listOf(start to "start",end to "end")) {
            val rect=actions[label]?.boundsInRoot ?: continue
            tap(rect.center)
            check(calls.last()==callback)
            tap(Offset(rect.left+3,rect.top+3))
            check(calls.last()==callback)
        }
        check(calls==when(count){0->emptyList();1->listOf("end","end");else->listOf("start","start","end","end")})
        println("PASS top bar width=$width RTL=$rtl scale=$scale actions=$count: heading + full semantic title, two-line overflow, logical placement, 48dp no overlap, raster and exact callbacks")
    } finally{scene.close();surface.close()}
}
fun main(args:Array<String>) {
    val result=runCatching {
        val mode=args.singleOrNull()
        require(mode in setOf(null,"blank-title","blank-action","duplicate"))
        probe(false,1f,320,2,mode)
        if(mode==null){
            probe(true,1.6f,180,2,null)
            probe(true,1.6f,144,2,null)
            probe(true,1.6f,84,2,null)
            probe(true,1.6f,84,1,null)
            probe(false,1f,144,0,null)
        }
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
