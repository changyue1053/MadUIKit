# MadUIKit 开发预览（源码审阅）

源码基线：`35c7a39b334ec6fa35c0329a6a3e4dd64d9855dc`。新增静态容器 `MadCard`；预留坐标 `0.1.0-alpha14-sdk-preview01`，未打包或发布。既有 API、依赖、消费者坐标不变。

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
implementation("com.mad.maduikit:maduikit-components:0.1.0-alpha14-sdk-preview01")
```

独立 Android 消费者须设置 `android.useAndroidX=true`（消费者的 `gradle.properties`，或 Gradle 命令的 `-Pandroid.useAndroidX=true`）；生产者配置不会传递给消费者。只需主题与排版类型时可换成 `maduikit-theme`；通过 components 的传递依赖也可读取主题。不要使用 `mavenLocal()`、单独拷贝 AAR 或混用不同版本目录。Android/iOS 的目标资格分别核对，KLIB 存在不代表应用已在设备运行。已提供接收 App 自有 Painter 的 `MadIcon` 和 `MadIconButton`，不内置图标素材。圆角与边框宽度已提供显式可选角色，旧控件接入及完整状态矩阵尚未交付。构建与回退见 [SDK-PREVIEW.md](SDK-PREVIEW.md)。

本地 Compose Desktop 冒烟测试覆盖开关角色、受控状态、禁用、长标签布局和指针点击。它不代表 TalkBack、VoiceOver 或鸿蒙原生无障碍通过，也没有三端运行截图。局部排版与间距的既有证据不等于此新坐标已编译或发布。

图标不内置资源。新版图标测试在 Compose Desktop 验证名称、角色、单一操作、按钮边缘点击和禁用行为；不代表 Android、iOS 或鸿蒙原生运行或无障碍验收。既有局部测量证据不代表此新坐标已构建、封装或发布。
