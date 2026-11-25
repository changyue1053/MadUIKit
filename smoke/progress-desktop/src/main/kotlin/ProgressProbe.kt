import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadLinearProgress
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun probe(rtl: Boolean, scale: Int, mode: String?) {
    val owners=mutableSetOf<SemanticsOwner>()
    val host=object : PlatformContext by PlatformContext.Empty {
        override val semanticsOwnerListener=object : PlatformContext.SemanticsOwnerListener {
            override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) { owners.add(semanticsOwner) }
            override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) { owners.remove(semanticsOwner) }
            override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {}
            override fun onLayoutChange(semanticsOwner: SemanticsOwner, semanticsNodeId: Int) {}
        }
    }
    val scene=CanvasLayersComposeScene(density=Density(scale.toFloat()),size=IntSize(180*scale,60*scale),platformContext=host)
    val surface=Surface.makeRasterN32Premul(180*scale,60*scale)
    val background=Color(0xffaa2244)
    val track=Color(if(rtl) 0xff304050 else 0xffe0eee2)
    val fill=Color(if(rtl) 0xffb1ed8a else 0xff14573a)
    val theme=if(rtl) darkColorScheme(primary=fill,secondaryContainer=track)
        else lightColorScheme(primary=fill,secondaryContainer=track)
    val value=mutableStateOf<Float?>(when(mode) {
        "negative" -> -.001f
        "above" -> 1.001f
        "nan" -> Float.NaN
        "infinite" -> Float.POSITIVE_INFINITY
        "minus-infinite" -> Float.NEGATIVE_INFINITY
        else -> 0f
    })
    val title=mutableStateOf<String?>(if(mode=="blank") " " else if (rtl) "تحميل الصور" else "Uploading photos")
    val widthDp=if(rtl) 73 else 100
    scene.setContent {
        MaterialTheme(colorScheme=theme) {
            CompositionLocalProvider(LocalLayoutDirection provides if(rtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                Box(Modifier.fillMaxSize().background(background).padding(10.dp)) {
                    MadLinearProgress(value.value, Modifier.width(widthDp.dp),label=title.value)
                }
            }
        }
    }
    var t=0L
    fun frame() { repeat(3) { surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),t);t+=20_000_000L } }
    data class Raster(val x: Int,val y: Int,val w: Int,val h: Int,val mid: List<Int>,val full: List<Int>)
    fun inspect(expected: Float?): Raster {
        val semantics=owners.single()
        val raw=semantics.getAllSemanticsNodes(mergingEnabled=false)
        val merged=semantics.getAllSemanticsNodes(mergingEnabled=true)
        check(raw.count{it.config.contains(SemanticsProperties.ProgressBarRangeInfo)}==1)
        val node=merged.single{it.config.contains(SemanticsProperties.ProgressBarRangeInfo)}
        check(node.config[SemanticsProperties.ProgressBarRangeInfo] ==
            if(expected==null) ProgressBarRangeInfo.Indeterminate else ProgressBarRangeInfo(expected,0f..1f))
        check(node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()==listOfNotNull(title.value))
        check((raw+merged).none{it.config.contains(SemanticsActions.OnClick) ||
            it.config.contains(SemanticsProperties.LiveRegion)})
        val r=node.boundsInRoot
        val l=r.left.toInt(); val top=r.top.toInt()
        val w=r.width.toInt(); val h=r.height.toInt()
        check(w==widthDp*scale && h==4*scale)
        Bitmap().use { b ->
            b.allocN32Pixels(180*scale,60*scale);check(surface.readPixels(b,0,0))
            fun p(x:Int,y:Int)=b.getColor(x,y)
            val full=(top until top+h).flatMap { y -> (l until l+w).map { x -> p(x,y) } }
            check((l-1..l+w).all{p(it,top-1)==background.toArgb() && p(it,top+h)==background.toArgb()})
            check((top..top+h).all{p(l-1,it)==background.toArgb() && p(l+w,it)==background.toArgb()})
            val mid=(l until l+w).map { p(it,top+h/2) }
            return Raster(l,top,w,h,if(rtl) mid.reversed() else mid,full)
        }
    }
    fun state(next:Float?):Raster { value.value=next;frame();return inspect(next) }
    try {
        frame()
        val zero=inspect(0f)
        check(zero.full.none{it==fill.toArgb()} && zero.mid[zero.w/2]==track.toArgb())
        val quarter=state(.25f)
        check(quarter.mid[quarter.w/8]==fill.toArgb() && quarter.mid[quarter.w/2]==track.toArgb())
        val full=state(1f)
        check(full.full.none{it==track.toArgb()} && full.mid[full.w/2]==fill.toArgb())
        val unknown=state(null)
        for ((pos,color) in listOf(.08f to track,.20f to fill,.44f to track,.68f to fill,.90f to track))
            check(unknown.mid[(unknown.w*pos).toInt()]==color.toArgb())
        for (delay in listOf(400_000_000L,1_600_000_000L,4_100_000_000L)) {
            t+=delay;frame();check(inspect(null).full==unknown.full) { "stationary indeterminate drew motion" }
        }
        title.value=null;frame();inspect(null)
        check(state(0f).full==zero.full)
        println("PASS progress RTL=$rtl density=${scale}x width=$widthDp: zero no fill, quarter direction, full no track, unknown two stationary segments, single label/semantic and no edge bleed")
    } finally {scene.close();surface.close()}
}

fun main(args:Array<String>) {
    val result=runCatching {
        val mode=args.singleOrNull()
        require(mode in setOf(null,"negative","above","nan","infinite","minus-infinite","blank"))
        probe(false,1,mode)
        if(mode==null) probe(true,2,null)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
