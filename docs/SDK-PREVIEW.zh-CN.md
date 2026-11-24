# MadUIKit S1 开发预览（源码审阅）

源码基线：`49f2c1f07bccaf2036ed5bdafe53d6412da45668`。本源码预览仅新增受控二元选择项 `MadCheckbox`，不改变 alpha06 及之前的接口或坐标；新预留坐标为 `0.1.0-alpha07-sdk-preview01`，未发布 Maven 包。

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
implementation("com.mad.maduikit:maduikit-components:0.1.0-alpha07-sdk-preview01")
```

独立 Android 消费者须设置 `android.useAndroidX=true`（消费者的 `gradle.properties`，或 Gradle 命令的 `-Pandroid.useAndroidX=true`）；生产者配置不会传递给消费者。只需主题与排版类型时可换成 `maduikit-theme`；通过 components 的传递依赖也可读取主题。不要使用 `mavenLocal()`、单独拷贝 AAR 或混用不同版本目录。Android/iOS 的目标资格分别核对，KLIB 存在不代表应用已在设备运行。图标、圆角、边框和完整状态矩阵未在这一步交付。构建与回退见 [SDK-PREVIEW.md](SDK-PREVIEW.md)。

本地 Compose Desktop 冒烟测试覆盖开关角色、受控状态、禁用、长标签布局和指针点击。它不代表 TalkBack、VoiceOver 或鸿蒙原生无障碍通过，也没有三端运行截图。局部排版与间距的既有证据不等于此新坐标已编译或发布。

图标不内置资源。新版图标测试在 Compose Desktop 验证名称、角色、单一操作、按钮边缘点击和禁用行为；不代表 Android、iOS 或鸿蒙原生运行或无障碍验收。既有局部测量证据不代表此新坐标已构建、封装或发布。
