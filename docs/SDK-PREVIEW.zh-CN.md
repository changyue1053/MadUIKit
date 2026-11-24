# MadUIKit S1 开发预览（源码审阅）

本分支把已测试的四个 S1 源文件放回 SDK 本身，不再靠 Demo 在 CI 中修改 SDK。基线为 SDK 提交 `19c8c1b1fbe75b7d6a6118fdab42048948de091b`。未来若另行封装这份源码，专用坐标为 `0.1.0-alpha03-sdk-preview01`，避免把新产物放到原 `0.1.0-alpha01` 或既有 `0.1.0-alpha02` 名下。**本分支不是二进制发布，也没有可下载的新 Maven 包。** 旧预览、旧 SDK 和 Demo 的版本锁定都不变。

| 能力 | 接入口 | 边界 |
|---|---|---|
| 间距 | `MadSpacingDefaults.Standard`（4/8/16 dp）或自定义 `MadSpacing` | 由 App 主动读取，如 `Modifier.padding(spacing.mediumDp.dp)`；不会改变现有组件布局 |
| 排版 | `MadTypography(body, title, label)` + `MadProvideTypography` | App 提供 Compose `TextStyle`。Provider 仅在子树内覆盖 Material 3 的三个对应角色；同子树读取这些角色的宿主控件也会受影响 |
| 文本 | `MadText` + `MadTextRole.Body/Title/Label` | 读取最近的主题角色，不携带字体，不承诺原生字体缩放或显示一致性 |
| 原有主题及控件 | `MadTheme`、`MadButton`、`MadFilterTabs` | 调用和默认路径不改。已有 App 按需要排版时，可只包目标子树，不必把整个 App 改成 `MadTheme` |

如已取得本预览坐标的**经校验的独立 Maven 暂存目录**，在宿主的 `dependencyResolutionManagement.repositories` 中把 `com.mad.maduikit` 独占指向此目录（`exclusiveContent`），保留 `google()` / `mavenCentral()` 给其他依赖。组件调用用：

```kotlin
implementation("com.mad.maduikit:maduikit-components:0.1.0-alpha03-sdk-preview01")
```

独立 Android 消费者须设置 `android.useAndroidX=true`（消费者的 `gradle.properties`，或 Gradle 命令的 `-Pandroid.useAndroidX=true`）；生产者配置不会传递给消费者。只需主题与排版类型时可换成 `maduikit-theme`；通过 components 的传递依赖也可读取主题。不要使用 `mavenLocal()`、单独拷贝 AAR 或混用不同版本目录。Android/iOS 的目标资格分别核对，KLIB 存在不代表应用已在设备运行。图标、圆角、边框和完整状态矩阵未在这一步交付。构建与回退见 [SDK-PREVIEW.md](SDK-PREVIEW.md)。
