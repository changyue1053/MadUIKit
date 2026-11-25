# MadUIKit 开发预览（源码审阅）

源码基线：`40cef1105c2a05514bcb658e30fdabb74c784d53`。新增 `MadSearchFieldWithClear`；预留 `0.1.0-alpha26-sdk-preview01`，未打包或发布。旧 API 不变。

## 搜索清除

独立命名的清除按钮只在可编辑且内容非空时显示。宿主可拒绝；接受时显式提供空的 TextFieldValue，完成当前编辑事务。触控不会请求焦点变化，键盘用户可分别聚焦和激活；清除不触发 Search 或显式关闭键盘。152 dp 以下按钮移到下方末端，给输入框与 48 dp 按钮留足空间。不同页面身份但同名字段请用宿主稳定 key。Desktop 合成 Compose 会话及键测试不代表系统输入法、原生键盘或读屏。

```kotlin
MadSearchFieldWithClear("搜索项目", query, { query = it }, submitSearch,
    "清除搜索", clearPainter, { query = TextFieldValue("") })
```


## 装饰输入框

宿主传入纯装饰的前后 Painter，限制在 24 dp；不会添加操作、焦点或辅助名称，点到图标区域仍可能聚焦输入框。144 dp 以下隐藏两侧图标；144 至 200 dp 不含上限时只显示一个，优先逻辑起始。示例占位符不替代标签、帮助或错误。保留完整 TextFieldValue，不跨端承诺系统键盘/IME/读屏。需要独立按钮时请在输入框旁另放可访问的按钮。

```kotlin
MadDecoratedTextField("名称", value, { value = it }, placeholder = "示例", leading = icon)
```


## 多行输入

宿主即时原样保存完整 `TextFieldValue`。minVisibleLines/maxVisibleLines 控制可见编辑高度，不限制字符或实际行数；需要足够空间承载标签和帮助。占位符仅是视觉示例，不承担读屏标签或必需说明；标题、支持文字和具体错误由宿主本地化，错误替换帮助。普通回车用于换行，显式提交/验证和键盘动作由宿主决定。此版没有安全输入、插槽或密码操作。Desktop 实测支持上述 Compose 行为，不等于原生输入法、软键盘、读屏或设备验收。

```kotlin
MadTextArea(
    label = "项目备注", value = notes, onValueChange = { notes = it },
    minVisibleLines = 2, maxVisibleLines = 4,
)
```


## 底部弹层

仅提供标题、正文和一个本地化关闭按钮。关闭按钮只发出请求，宿主自己更新 `visible`。不提供拖动或半展开状态；组件关闭了返回与外部点击触发关闭的配置。内容从底部展开、最高 480 dp，正文可滚动；须保留足够高的宿主区域以容纳标题和固定关闭按钮。弹层保留固定版本 Material3 的默认内容边距，这不等于原生安全区或键盘验收。Desktop 原生 Compose 图层测试了 Escape、遮挡点击和正文轮滚。未验收系统返回、触摸手势、屏幕阅读器、焦点恢复、安全区或键盘；不代表移动端可用。

```kotlin
MadBottomSheet(
    visible = showingDetails, title = "传输详情", message = detailText,
    closeLabel = "关闭", onCloseRequest = { showingDetails = false },
)
```


## 页内提示条

提示条与页面内容并存，不替代加载、空或错误面板。宿主提供 Neutral、Warning 或 Error、本地化状态名称和正文；文字必须表达严重程度，不能只靠颜色。可提供一个按钮作为后续操作或关闭意图，显示和移除由宿主控制。不同通知若文案相同而业务身份不同，请在外层用稳定通知 ID 做 Compose key。无自动关闭、自动播报、悬浮或全局队列。窄宽长文需要足够纵向空间；仅有 Desktop Compose/Skia 证据，不代表原生端读屏。

```kotlin
MadBanner(
    tone = MadBannerTone.Warning, statusLabel = "注意", message = "此文件夹只读。",
    action = MadBannerAction("关闭提示", dismissNotice),
)
```


## 加载、空和错误状态

宿主提供 Loading、Empty 或 Error、该状态的本地化标题及可选正文。Loading 使用静态不定进度，不显示操作；Empty/Error 可显示一个宿主决定的按钮。组件不主动播报、不访问网络、不调度任务或重试。长文字可换行；紧凑容器需给予纵向空间或自己提供滚动，不把内容固定在过矮区域。本地 Desktop Compose/Skia 语义和像素验证不代表原生读屏或设备。

```kotlin
MadStatePanel(
    state = MadPanelState.Error, title = "无法加载项目",
    action = MadStatePanelAction("重试", onRetry),
)
```


## 顶部栏

宿主提供标题、可选逻辑起始导航按钮及可选末端按钮。每个按钮由 Painter、本地化名称和宿主回调组成；无任意操作槽。宿主决定路由、系统边距与需要镜像的画符。长标题至多两行，视觉省略但保留完整标题语义。宽度不够时操作移到标题下方；两项且宽度小于 96 dp 时分行避免重叠。触控目标要求至少 48 dp 可用宽度。没有折叠、滚动联动或平台焦点策略。仅有 Desktop 验证，不代表原生读屏、安全区或 S3 封板。

```kotlin
MadTopAppBar(
    title = "工作空间",
    navigation = MadTopAppBarAction(backPainter, "返回", onBack),
)
```


## 确认弹窗

基于 CMP Material3 AlertDialog。宿主分别处理确认、取消和环境关闭请求，三个动作都不自动改 `visible`。默认 Back/Escape 请求关闭，外部点击不请求关闭；可显式配置。弹窗可见时标题、正文与两个按钮文案须本地化、非空，按钮文案须能区分。不负责导航、业务结果或焦点恢复。Desktop Compose 图层验证了 Escape、遮挡点击、三个回调和宿主关闭；不证明移动端系统返回、焦点恢复、原生读屏或其他平台。

```kotlin
MadConfirmDialog(
    visible = showing, title = "归档项目？", message = "可稍后在设置中恢复。",
    confirmLabel = "归档", cancelLabel = "保留项目",
    onConfirm = requestArchive, onCancel = cancelArchive, onDismissRequest = dismissPrompt,
)
```


## 线性进度

传入有限且在 0–1 之间的比例确定进度，0 只显示轨道，1 为全填充。未知进度传 `null`，用两段静态图案区分，不代表百分比。组件不播放动画，因此不读系统减弱动态设置；也不做业务调度、增量播报或回调。只在相邻内容没有命名时传入本地化 `label`；如同页有多个任务，应给各自明确名称。填充随 RTL 镜像；主题采用 primary/secondaryContainer，自定义主题需保证区分度。仅 Desktop 已验证，不代表原生读屏或平台设置。

```kotlin
MadLinearProgress(fraction = uploadFraction, label = "上传照片")
// uploadFraction: Float?; null means an unknown amount.
```


## S3 底部导航原型

底部目的地栏接收 2–5 个条目、稳定 ID、宿主本地化标签和装饰性 Painter。宿主拥有当前 ID、路由、返回栈和系统栏留白；SDK 只报告未选中且启用条目的意图。已选中或禁用项不回调；在宿主接受状态前，重复触发仍各发一条请求，可由宿主节流。选中的禁用项仍显示选中且不可操作。每项建议至少 48 dp 宽；标签至多两行，视觉可省略但语义保留全称。五项导航在 240 dp（每项 48 dp）、RTL、1.6 倍字体和长译文下已通过 Desktop 验证。不代表物理键盘 Tab/方向键、原生读屏、系统边距或 S3 封板验收。

```kotlin
MadNavigationBar(
    items = listOf(
        MadNavigationItem("home", homeLabel, homePainter),
        MadNavigationItem("profile", profileLabel, profilePainter),
    ),
    selectedId = currentDestinationId,
    onDestinationRequest = requestDestination,
)
```

## 搜索输入

复用现有 `MadTextField`。宿主即时保存完整 `TextFieldValue`，不丢弃光标、选区和 composing 区间。仅可编辑状态下收到 Search 动作才回调；输入、只读或禁用不自动搜索。标签、字段下方帮助（不是行内占位符）和可替换帮助的错误，由宿主本地化。组件不请求网络、去抖、清焦或保存结果；暂不提供清除按钮/视觉提交按钮，宿主可单独实现并决定编辑会话边界。桌面合成 Compose 输入会话验证，不等同系统键盘/原生读屏/移动设备验收。

```kotlin
MadSearchField(
    label = "搜索项目", value = query,
    onValueChange = { query = it }, onSearch = { submit(query.text) },
)
```

回退：将调用切回 alpha15 已有的 `MadTextField`，保留同一完整值，使用 `KeyboardOptions(imeAction = ImeAction.Search)` 和宿主自行负责启停的 `KeyboardActions(onSearch = { ... })`。仅使用先前的独立 alpha15 源码或已保留的原构件，不以新源码重打 alpha15。


## 头像

宿主传入已有的 `Painter`（优先）或事先按语言格式化的单行首字，SDK 不截取姓名、不下载或打包图片。40 dp 默认空间；长方形约束中按短边绘制居中圆形。放不下的首字不裁切，显示主题色小圆点。头像不提供操作。装饰用途须显式传 `contentDescription = null`；有独立含义时传本地化名称，避免与相邻姓名重复。主题采用 `primaryContainer/onPrimaryContainer`。验证限 Desktop Compose/Skia，不代表原生读屏或设备。

```kotlin
MadAvatar(contentDescription = null, painter = photo, initials = "林")
MadAvatar(contentDescription = "林女士的个人资料", initials = "林")
```


## 内容卡片

卡片提供静态容器和受裁剪的 Column 内容槽。由宿主决定宽高、内部留白、内容及子控件操作；组件不自行整卡点击、合并语义或绘制阴影。使用最近主题的 `surfaceContainer`、`onSurface` 和 `outlineVariant`；内容色只作用于卡片内部。默认 Medium 圆角、无描边；可用现有几何角色启用描边。描边画在卡片区域内，必要时请留内边距。仅有 Desktop Compose/Skia 证明，不代表 Android/iOS/鸿蒙。

```kotlin
MadCard(modifier = Modifier.fillMaxWidth(), borderRole = MadBorderRole.Subtle) {
    Column(Modifier.padding(MadSpacingDefaults.Standard.largeDp.dp)) {
        MadText("账户状态", role = MadTextRole.Title)
    }
}
```


## 列表行

静态行省略 `onClick`，无操作、按钮角色或“已禁用”标记；`enabled` 只在传入操作时生效。有操作时整行只有一个动作；禁用阻止回调。标题最多 2 行，补充文字最多 3 行，超出视觉省略，语义保留完整文字。文字须由宿主本地化。此版无自定义槽位、内嵌按钮或导航。宿主掌握列表、选中状态、滚动及稳定键，勿用索引作会重排的条目的键。样例只证明 Desktop Compose/Skia，不是原生设备或辅助技术验证。

```kotlin
LazyColumn {
    items(rows, key = { it.id }) { row ->
        MadListItem(label = row.title, supportingText = row.subtitle) { open(row.id) }
    }
}
```


## 单行输入

及时保存回调的完整 `TextFieldValue`，包括选区和 composing 区间；不要只取 `text` 后重建，也不应等待异步校验才回填。可另外取字符串供业务使用。标签、提示、错误由宿主本地化；错误替换提示并提供具体错误语义。禁用不能编辑或聚焦；启用的只读字段仍可聚焦和选中。圆角采用现有 Medium 角色；文本、描边和状态采用最近的 Material 主题。宿主用 `KeyboardOptions`、`KeyboardActions` 与焦点 Modifiers 编排 Next/Done。桌面验证使用真实 Compose 平台输入会话注入合成命令，不证明系统键盘、原生读屏或 Android/iOS/鸿蒙。密码、格式化、多行、富文本和平台键盘调优不在范围内。

```kotlin
var value by remember { mutableStateOf(TextFieldValue("")) }
MadTextField(
    label = "显示名称", value = value,
    onValueChange = { value = it },
    errorMessage = validationMessage, // localized or null
)
```


## 状态与数量徽标

徽标可显示状态或数量。使用主题的 `secondaryContainer/onSecondaryContainer` 语义色和 `labelLarge` 排版；已有应用可继续用 `MadProvideTypography` 覆盖排版。数量、99+ 上限和本地化由宿主决定。默认公开可见文字；`spokenLabel` 可替换朗读文本而非叠加名称。组件无点击、关闭或主动播报；如果徽标放在可点击父项中，由父项决定合并后的描述。正常约束下允许换行和增高，请勿依赖固定高度。圆角固定上限为 8 dp，不随徽标增高而继续扩大。仅有 Desktop Compose/Skia 结果，不是原生读屏或设备认证。

```kotlin
MadBadge(text = "新内容")
MadBadge(text = "12", spokenLabel = "12 条未读消息")
```


## 按钮变体与加载

显式选择 Primary（填充）、Secondary（描边）、Text（纯文字）或 Danger（错误色）。传入非空且已本地化的 `label`；`loadingStatus = null` 表示未加载，传入本地化非空字符串表示正在加载。加载和禁用均阻止回调；两者同时生效时仍保留加载描述和装饰性进度图。宿主应在回调中及时更新状态并负责业务防重；SDK 不自动排队或防抖。加载描述不是第二个名称；不要在 Modifier 上重复设置名称或操作。此次仅验证 Desktop Compose 与 Skia，不代表原生读屏或设备验证。

```kotlin
MadActionButton(
    label = "保存", variant = MadButtonVariant.Primary,
    onClick = requestSave,
    loadingStatus = if (saving) "正在保存" else null,
)
```


## 几何角色

宿主可用 `RoundedCornerShape(MadGeometryDefaults.CornerRadii[MadCornerRole.Medium].dp)`，或传入自己的 `MadCornerRadii(0, 6, 10)`。角色按从小到大排列，0 表示直角。边框宽度必须为正且按角色排序（允许相等）；默认 Subtle/Strong 为 1/2 dp，0 不表示 hairline。例：`MadDivider(borderWidths = MadBorderWidths(2, 3), role = MadBorderRole.Strong)`。分割线无操作或名称；在有界父项中铺满可用宽度，无界时请指定宽度。限制高度后不会越界绘制。Desktop Skia 像素验证不代表 Android、iOS 或鸿蒙验收。圆角、边框目前只提供可选几何角色，不承诺旧控件统一接入或完整状态矩阵。

## 单选组

用 `Modifier.selectableGroup()` 标注组。宿主只保存一份当前选项；仅启用且未选中的一行触发 `onSelect`，已选中或禁用时不回调。传入本地化非空标签；约束允许时，整行至少 48 × 48 dp。Desktop 验证了 Compose 语义和触摸，不代表原生读屏或设备验收。

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mad.maduikit.MadRadio

@Composable
fun DisplayOptions(mode: String, onModeChange: (String) -> Unit) {
    Column(Modifier.selectableGroup()) {
        MadRadio("Auto", selected = mode == "auto", onSelect = { onModeChange("auto") })
        MadRadio("Manual", selected = mode == "manual", onSelect = { onModeChange("manual") })
    }
}
```

`MadCheckbox(label = "离线阅读", checked = saved, onCheckedChange = { saved = it })`。只在宿主更新 `checked` 后改变选中态；不适合三态或互斥选择。提供本地化非空标签，不要在外部 Modifier 添加第二个名称或操作。约束允许时整行至少 48 × 48 dp。禁用时不触发回调。桌面 smoke 验证 Compose 语义树与触摸，不代表 TalkBack、VoiceOver、Android、iOS 或鸿蒙验收。

| 能力 | 接入口 | 边界 |
|---|---|---|
| 间距 | `MadSpacingDefaults.Standard`（4/8/16 dp）或自定义 `MadSpacing` | 由 App 主动读取，如 `Modifier.padding(spacing.mediumDp.dp)`；不会改变现有组件布局 |
| 排版 | `MadTypography(body, title, label)` + `MadProvideTypography` | App 提供 Compose `TextStyle`。Provider 仅在子树内覆盖 Material 3 的三个对应角色；同子树读取这些角色的宿主控件也会受影响 |
| 文本 | `MadText` + `MadTextRole.Body/Title/Label` | 读取最近的主题角色，不携带字体，不承诺原生字体缩放或显示一致性 |
| 开关 | `MadSwitch(label, checked, onCheckedChange, modifier, enabled)` | 标签必须非空且由 App 本地化；App 决定状态，点击只提出变更；禁用时不会触发指针回调。标签与图形共用一个开关动作，长标签可换行 |
| 图标与图标按钮 | `MadIcon`、`MadIconButton` | App 提供 Painter；普通图标可用 null 标识装饰，按钮需非空本地化操作名称；整个按钮只有一个操作。父布局允许时，默认触控目标至少 48dp |
| 原有主题及控件 | `MadTheme`、`MadButton`、`MadFilterTabs` | 调用和默认路径不改。已有 App 按需要排版时，可只包目标子树，不必把整个 App 改成 `MadTheme` |

如已取得本预览坐标的**经校验的独立 Maven 暂存目录**，在宿主的 `dependencyResolutionManagement.repositories` 中把 `com.mad.maduikit` 独占指向此目录（`exclusiveContent`），保留 `google()` / `mavenCentral()` 给其他依赖。组件调用用：

```kotlin
implementation("com.mad.maduikit:maduikit-components:0.1.0-alpha26-sdk-preview01")
```

独立 Android 消费者须设置 `android.useAndroidX=true`（消费者的 `gradle.properties`，或 Gradle 命令的 `-Pandroid.useAndroidX=true`）；生产者配置不会传递给消费者。只需主题与排版类型时可换成 `maduikit-theme`；通过 components 的传递依赖也可读取主题。不要使用 `mavenLocal()`、单独拷贝 AAR 或混用不同版本目录。Android/iOS 的目标资格分别核对，KLIB 存在不代表应用已在设备运行。已提供接收 App 自有 Painter 的 `MadIcon` 和 `MadIconButton`，不内置图标素材。圆角与边框宽度已提供显式可选角色，旧控件接入及完整状态矩阵尚未交付。构建与回退见 [SDK-PREVIEW.md](SDK-PREVIEW.md)。

本地 Compose Desktop 冒烟测试覆盖开关角色、受控状态、禁用、长标签布局和指针点击。它不代表 TalkBack、VoiceOver 或鸿蒙原生无障碍通过，也没有三端运行截图。局部排版与间距的既有证据不等于此新坐标已编译或发布。

图标不内置资源。新版图标测试在 Compose Desktop 验证名称、角色、单一操作、按钮边缘点击和禁用行为；不代表 Android、iOS 或鸿蒙原生运行或无障碍验收。既有局部测量证据不代表此新坐标已构建、封装或发布。
