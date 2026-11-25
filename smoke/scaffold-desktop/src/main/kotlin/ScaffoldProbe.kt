import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import com.mad.maduikit.MadScaffold
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Surface

@OptIn(InternalComposeUiApi::class)
private fun exercise(rtl:Boolean,mode:String?){
    val owners=mutableSetOf<SemanticsOwner>()
    val host=object:PlatformContext by PlatformContext.Empty{
        override val semanticsOwnerListener=object:PlatformContext.SemanticsOwnerListener{
            override fun onSemanticsOwnerAppended(semanticsOwner:SemanticsOwner){owners.add(semanticsOwner)}
            override fun onSemanticsOwnerRemoved(semanticsOwner:SemanticsOwner){owners.remove(semanticsOwner)}
            override fun onSemanticsChange(semanticsOwner:SemanticsOwner){}
            override fun onLayoutChange(semanticsOwner:SemanticsOwner,semanticsNodeId:Int){}
        }
    }
    val pixels=IntSize(340,340)
    val scene=CanvasLayersComposeScene(density=Density(1f),size=pixels,platformContext=host)
    val surface=Surface.makeRasterN32Premul(pixels.width,pixels.height)
    val hostWidth=mutableStateOf(160)
    val hostHeight=mutableStateOf(200)
    val top=mutableStateOf(true)
    val bottom=mutableStateOf(true)
    val outside=Color(0xff7e2463)
    val color=mapOf("top" to Color(0xff117e66),"body" to Color(0xffdebf66),"bottom" to Color(0xff5967ab))
    val calls=mutableListOf<String>()
    var count=0
    var lastBody=0
    @Composable fun slot(name:String,modifier:Modifier,click:()->Unit){
        Box(modifier.clickable(interactionSource=null,indication=null,role=Role.Button,onClick=click).semantics { contentDescription=name }
            .drawBehind {
                // Deliberately misbehaving child; the parent slot must own clipping.
                drawRect(color.getValue(name),Offset(-20f,-20f),Size(size.width+40,size.height+40))
            })
    }
    @Composable fun content(){
        val id=remember { ++count }
        lastBody=id
        slot("body",Modifier.fillMaxSize()){calls.add("body:$id")}
    }
    scene.setContent {
        CompositionLocalProvider(LocalLayoutDirection provides if(rtl) LayoutDirection.Rtl else LayoutDirection.Ltr){
            MaterialTheme {
                Box(Modifier.fillMaxSize().background(outside).padding(10.dp)){
                    if(mode=="unbounded"){
                        Box(Modifier.verticalScroll(rememberScrollState())) { MadScaffold { content() } }
                    } else {
                        MadScaffold(modifier=Modifier.size(hostWidth.value.dp,hostHeight.value.dp).padding(7.dp),
                            topBar=if(top.value) { {slot("top",Modifier.fillMaxWidth().height(54.dp)){calls.add("top")}} } else null,
                            bottomBar=if(bottom.value) { {slot("bottom",Modifier.fillMaxWidth().height(48.dp)){calls.add("bottom")}} } else null,
                            content={content()})
                    }
                }
            }
        }
    }
    var now=0L
    fun frame(){repeat(6){surface.canvas.clear(0);scene.render(surface.canvas.asComposeCanvas(),now);now+=20_000_000L}}
    fun named()=owners.single().getAllSemanticsNodes(mergingEnabled=true).mapNotNull{n->
        n.config.getOrNull(SemanticsProperties.ContentDescription)?.singleOrNull()?.let{it to n}
    }.toMap()
    fun tap(x:Float,y:Float){
        val p=Offset(x,y)
        scene.sendPointerEvent(PointerEventType.Press,p,timeMillis=now/1_000_000,type=PointerType.Touch)
        scene.sendPointerEvent(PointerEventType.Release,p,timeMillis=now/1_000_000+1,type=PointerType.Touch)
        frame()
    }
    fun snapshot(expectTop:Int,expectBody:Int,expectBottom:Int):Map<String,SemanticsNode>{
        val h=hostHeight.value-14; val w=hostWidth.value-14
        val x=if(rtl) pixels.width-17-w else 17
        val y=17
        val runs=listOf("top" to expectTop,"body" to expectBody,"bottom" to expectBottom)
        check(runs.sumOf{it.second}==h && count==1 && lastBody==1)
        Bitmap().use{b->
            b.allocN32Pixels(pixels.width,pixels.height);check(surface.readPixels(b,0,0))
            val expected=runs.flatMap{(name,n)->List(n){color.getValue(name).toArgb()}}
            check(expected.indices.all{i->(x until x+w).all{xx->b.getColor(xx,y+i)==expected[i]}}) { "slot bleed into neighbor" }
            check((y-1..y+h).all{yy->b.getColor(x-1,yy)==outside.toArgb() && b.getColor(x+w,yy)==outside.toArgb()}) { "slot bleed through host padding" }
            check((x-1..x+w).all{xx->b.getColor(xx,y-1)==outside.toArgb() && b.getColor(xx,y+h)==outside.toArgb()})
        }
        val n=named()
        var yy=y
        for((name,height) in runs){
            val bb=n[name]?.boundsInRoot
            if(height>0) check(bb!=null && bb.left==x.toFloat() && bb.top==yy.toFloat() &&
                 bb.height==height.toFloat() && bb.width==w.toFloat()) { "bad bounds: $name $bb" }
            else if (bb!=null) check(bb.height==0f)
            yy+=height
        }
        return n
    }
    fun tap(n:SemanticsNode){val p=n.boundsInRoot.center;tap(p.x,p.y); }
    try{
        frame()
        val first=snapshot(54,84,48)
        tap(first.getValue("top"));tap(first.getValue("body"));tap(first.getValue("bottom"))
        check(calls==listOf("top","body:1","bottom"))
        hostWidth.value=102;hostHeight.value=86;frame()
        val tight=snapshot(54,0,18)
        tap(tight.getValue("bottom"));check(calls.last()=="bottom") { "collapsed body intercepted bottom tap" }
        hostHeight.value=42;frame();snapshot(28,0,0)
        top.value=false;hostHeight.value=160;frame()
        val lower=snapshot(0,98,48);tap(lower.getValue("body"))
        check(calls.last()=="body:1")
        bottom.value=false;frame();snapshot(0,146,0)
        top.value=true;frame();val upper=snapshot(54,92,0);tap(upper.getValue("body"))
        check(calls.last()=="body:1" && calls.size==6)
        println("PASS scaffold RTL=$rtl: 0/1/2 slots, exact pixels and no bleed incl. 28/72-height, pointer ownership, host padding once and remembered content stable across resize")
    } finally {scene.close();surface.close()}
}

fun main(args:Array<String>){
    val result=runCatching{
        val mode=args.singleOrNull()
        require(mode in setOf(null,"unbounded"))
        exercise(false,mode)
        if(mode==null)exercise(true,null)
    }
    result.exceptionOrNull()?.printStackTrace()
    kotlin.system.exitProcess(if(result.isSuccess)0 else 1)
}
