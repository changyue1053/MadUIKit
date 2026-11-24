# MadUIKit

> **Source-only S1 preview:** This checkout uses the distinct future coordinate
> `0.1.0-alpha03-sdk-preview01`. No new Maven bundle has been published. This
> checkout does not build or replace the previous `0.1.0-alpha01` preview. See
> [isolated build and integration notes](docs/SDK-PREVIEW.md) and
> [中文接入说明](docs/SDK-PREVIEW.zh-CN.md).

Reusable Kotlin Multiplatform / Compose Multiplatform design-system components.
This milestone contains two runtime modules, with no Figma access, generator,
or app entry point.

| Module | Public API |
| --- | --- |
| `maduikit-theme` | `MadTheme`, `MadColors`, `MadColorDefaults`, `MadSpacing`, `MadSpacingDefaults`, `MadTypography`, `MadProvideTypography` |
| `maduikit-components` | `MadButton`, `MadFilterTabs`, `MadFilterTab`, `MadText`, `MadTextRole` |

The source configures Android (min API 23), iOS ARM64 devices and iOS ARM64
simulators. The iOS API is Kotlin/Compose, not a Swift-facing widget kit. This
branch is a source review, not evidence of a Maven publication, native app
execution, or production compatibility.

## Build requirements

Use JDK 17, Android SDK Platform 36 and Build Tools 35.0.0. Set `JAVA_HOME` and
`ANDROID_HOME` (or put the SDK location in untracked `local.properties`). The
wrapper pins Gradle 8.13 and the distribution checksum. A fresh build needs
access to the Gradle distribution, Gradle Plugin Portal, Google Maven and Maven
Central. This build uses Kotlin and its Compose compiler plugin 2.2.21, Compose
Multiplatform 1.9.3, Compose Material 3 1.9.0 and Android Gradle Plugin 8.11.1.

```sh
./gradlew :maduikit-theme:testDebugUnitTest \
  :maduikit-theme:assembleRelease :maduikit-components:assembleRelease
```

This is a source/build check, not a staging or consumption procedure.

The common contrast test covers both default accent pairs, not custom themes
or all states.

## Preview integration

For this checkout, follow the [isolated alpha03 staging and consumption notes](docs/SDK-PREVIEW.md)
only when a local build is authorized. The previous alpha01 preview is a separate,
already-staged artifact. If maintaining an existing alpha01 integration, use its
original package and [pinned instructions at the alpha01 source](https://github.com/changyue1053/MadUIKit/blob/19c8c1b1fbe75b7d6a6118fdab42048948de091b/README.md),
not this checkout or its stage.

### Controlled filter tabs

`MadFilterTabs` and `MadFilterTab` are unstyled Compose primitives with
grouped, selected/disabled tab semantics. The host owns selection, supplies
accessible labels, and can render any indicator in the Box slot. Use
one selected option and a suitably sized target; the components do not impose
density, colors, text, or a minimum layout size.

```kotlin
enum class BookFilter { All, Favorites }

@Composable
fun BookFilters(selected: BookFilter, onSelect: (BookFilter) -> Unit) {
    MadFilterTabs(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MadFilterTab(
            selected = selected == BookFilter.All,
            onSelect = { onSelect(BookFilter.All) },
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) { Text("All books", Modifier.align(Alignment.Center)) }
        MadFilterTab(
            selected = selected == BookFilter.Favorites,
            onSelect = { onSelect(BookFilter.Favorites) },
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
        ) { Text("Favorites", Modifier.align(Alignment.Center)) }
    }
}
```

Do not add a second click target to a tab's content. Place independent
actions outside the selectable group where the layout allows.

The standalone synthetic Desktop smoke invokes actual pointer events and checks
host-controlled selection and disabled behavior. It compiles only the relevant
SDK source, leaving the SDK's published target set unchanged:

```sh
./gradlew -p smoke/filter-tabs-desktop run
```
