# MadUIKit

Reusable Kotlin Multiplatform / Compose Multiplatform design-system components.
This milestone contains two runtime modules, with no Figma access, generator,
or app entry point.

| Module | Public API |
| --- | --- |
| `maduikit-theme` | `MadTheme`, `MadColors`, `MadColorDefaults` |
| `maduikit-components` | `MadButton`, `MadFilterTabs`, `MadFilterTab` |

The source configures Android (min API 23), iOS ARM64 devices and iOS ARM64
simulators. The iOS API is Kotlin/Compose, not a Swift-facing widget kit. This
checkpoint is an Android binary integration rehearsal: it does not publish
iOS artifacts, a remote Maven release, or a production compatibility promise.

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

The common contrast test covers both default accent pairs, not custom themes
or all states.

## Stage the Android SDK for the demo

From this repository's root, generate the KMP root metadata and Android
release AARs in a filesystem Maven repository:

```sh
./gradlew \
  :maduikit-theme:publishKotlinMultiplatformPublicationToMadUIKitLocalRepository \
  :maduikit-theme:publishAndroidReleasePublicationToMadUIKitLocalRepository \
  :maduikit-components:publishKotlinMultiplatformPublicationToMadUIKitLocalRepository \
  :maduikit-components:publishAndroidReleasePublicationToMadUIKitLocalRepository
```

This writes to the sibling directory `../maduikit-maven` by default. To use a
different directory, pass the same absolute
`-PmaduikitStagingDir=/absolute/path/to/maduikit-maven` to the SDK and Demo
commands. There is no remote publishing destination or `mavenLocal()`
requirement. The stage is not a Git checkout and should not be committed.

In a Kotlin/Compose consumer, configure the staging repository and add the
root module dependency in `commonMain` so Gradle selects the appropriate
variant:

```kotlin
implementation("com.mad.maduikit:maduikit-components:0.1.0-alpha01")
```

```kotlin
@Composable
fun ExampleAction(onConfirm: () -> Unit, canConfirm: Boolean) {
    MadTheme {
        MadButton(label = "Confirm", onClick = onConfirm, enabled = canConfirm)
    }
}
```

`MadTheme` and `MadButton` are in `com.mad.maduikit`. Callers provide
localized labels and own business actions. For custom accents, specify both the
accent and its contrasting text color. This sample staging command cannot
satisfy an iOS consumer.

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
