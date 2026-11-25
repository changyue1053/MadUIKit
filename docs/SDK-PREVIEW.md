# Source-native developer preview

Source baseline: `9bc7b6aca7ae721ddf1bbb1c6ff262e6cb332146`. Adds the read-only `MadLinearProgress`. The reserved coordinate is `0.1.0-alpha18-sdk-preview01`; no Maven bundle has been published. Earlier APIs, dependencies, and consumers are unchanged.

## Linear progress

Pass a finite fraction in 0–1 for determinate progress: 0 paints only the track and 1 paints full fill. Null paints two stationary segments and has indeterminate semantics, not a percentage. Neither mode animates or reads a system motion preference. The host owns scheduling and announcement policy; no callback is emitted. Use a localized `label` to name the operation when nearby content does not already do so. Fill follows RTL. Custom `primary/secondaryContainer` colors must remain distinguishable. Desktop illustration is not native screen-reader or platform settings qualification.

```kotlin
MadLinearProgress(fraction = uploadFraction, label = "Uploading photos")
// uploadFraction: Float?; null means an unknown amount.
```


## S3 bottom navigation prototype

A bottom destination bar takes 2–5 stable IDs with localized labels and decorative caller Painters. The host owns selected ID, routes/back stack and system insets. Only enabled, unselected items emit requests. Repeated activations request again until the host accepts; gate pending work in the host. A currently selected disabled item remains selected and inactive. Allocate at least 48 dp per item; labels have two visual lines and keep their complete semantic names. Desktop checks include five destinations at 240 dp (48 dp each), RTL, font scale 1.6 and long translated labels. This does not qualify keyboard Tab/arrow navigation, native screen readers or system insets, and does not finish S3.

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

## Search field

Uses the existing `MadTextField`. Store the complete `TextFieldValue` immediately, including caret, selection and composition. Only the Search action in an editable state invokes the callback; typing, read-only and disabled states do not search. Localize the label, supporting hint (below the field, not an inline placeholder) and an error that replaces that hint. No networking, debounce, focus change or result state. Clear and visual submit affordances are deferred to the host, which defines edit-session boundaries. Desktop synthetic Compose input sessions are not an OS keyboard, native accessibility or mobile device qualification.

```kotlin
MadSearchField(
    label = "Search items", value = query,
    onValueChange = { query = it }, onSearch = { submit(query.text) },
)
```

Rollback: use alpha15’s existing `MadTextField` with the same complete value, `KeyboardOptions(imeAction = ImeAction.Search)`, and a host-gated `KeyboardActions(onSearch = { ... })`. Use the prior pinned alpha15 source or previously retained exact artifacts; do not rebuild alpha15 from this checkout.


## Avatars

Supply a caller-owned `Painter` (preferred) or preformatted, localized one-line initials. The SDK does not parse names, fetch images or ship assets. Default allocation is 40 dp; inside a rectangle the circle is centered on the shorter side. Initials that do not fit are not clipped: a themed dot replaces them. The avatar is noninteractive. Explicitly use `contentDescription = null` for decoration; give a localized name only when it is not duplicated by nearby text. Uses `primaryContainer/onPrimaryContainer`. Desktop Compose/Skia evidence is not native accessibility or device qualification.

```kotlin
MadAvatar(contentDescription = null, painter = photo, initials = "LS")
MadAvatar(contentDescription = "Lina’s profile", initials = "LS")
```


## Content cards

A static surface with a clipped Column slot. The host owns size, inner padding, content and child actions. It does not merge semantics, add a whole-card action or draw shadows. The nearest theme supplies `surfaceContainer`, `onSurface` (scoped to the slot) and `outlineVariant`. Defaults to Medium corners and no border; opt into one of the existing border roles. The border overlays the inside edge, so pad the slot if necessary. Evidence is Desktop Compose/Skia, not Android/iOS/HarmonyOS.

```kotlin
MadCard(modifier = Modifier.fillMaxWidth(), borderRole = MadBorderRole.Subtle) {
    Column(Modifier.padding(MadSpacingDefaults.Standard.largeDp.dp)) {
        MadText("Account status", role = MadTextRole.Title)
    }
}
```


## List rows

Omit `onClick` for a static row with no action, button role, or disabled marker; `enabled` applies only to actionable rows. An actionable row exposes one whole-row action; disabling blocks callbacks. The label wraps up to two lines and support up to three, with visual ellipsis and full semantics. Localize both strings in the host. There are no custom slots, nested controls, or navigation. The host owns list state, selection, scrolling and stable keys; do not key reorderable items by their index. Scope is Desktop Compose/Skia, not native devices or assistive technology.

```kotlin
LazyColumn {
    items(rows, key = { it.id }) { row ->
        MadListItem(label = row.title, supportingText = row.subtitle) { open(row.id) }
    }
}
```


## Controlled single-line input

Store the complete `TextFieldValue` immediately, including selection and composition. Do not reconstruct it from `text` or wait on async validation before accepting the edit. Labels, help and errors are localized by the host; the specific error replaces visible help. Disabled fields cannot edit or focus; enabled read-only fields can focus and select. The existing Medium corner role is used; the nearest Material theme supplies text, outline and state colors. Configure Next/Done and focus order with `KeyboardOptions`, `KeyboardActions` and focus modifiers. Desktop uses real Compose platform input sessions with synthetic edit commands; it does not qualify an OS keyboard, native assistive technology, or Android/iOS/HarmonyOS. Passwords, transformations, multiline, rich text and platform keyboard tuning are out of scope.

```kotlin
var value by remember { mutableStateOf(TextFieldValue("")) }
MadTextField(
    label = "Display name", value = value,
    onValueChange = { value = it },
    errorMessage = validationMessage, // localized or null
)
```


## Status and count badges

A badge displays a status or caller-formatted count using theme `secondaryContainer/onSecondaryContainer` and `labelLarge`. The caller owns localization, count caps, and announcements. By default the visible text is exposed; `spokenLabel` replaces it in exposed semantics rather than adding a name. It is not clickable, dismissible, or a live region. An interactive parent owns merged labeling. Give it room to wrap and grow instead of forcing a fixed height. The corner radius is capped at 8 dp rather than increasing with wrapped height. Desktop Compose/Skia only; no native accessibility or device qualification.

```kotlin
MadBadge(text = "New")
MadBadge(text = "12", spokenLabel = "12 unread messages")
```


## Button variants and loading

Choose Primary (filled), Secondary (outlined), Text (unfilled), or Danger (error colors) explicitly. Supply a localized nonblank `label`. Set `loadingStatus` to null when idle or a localized nonblank string when loading. Loading or disabling suppresses activation; loading description and the decorative spinner remain during disabled+loading. The host updates state and owns business-layer deduplication; there is no internal queue or debounce. Do not add a second name or action through the modifier. Evidence is from Desktop Compose and Skia only, not native accessibility or device verification.

```kotlin
MadActionButton(
    label = "Save", variant = MadButtonVariant.Primary,
    onClick = requestSave,
    loadingStatus = if (saving) "Saving" else null,
)
```


## Opt-in geometry roles

Use `RoundedCornerShape(MadGeometryDefaults.CornerRadii[MadCornerRole.Medium].dp)` or provide `MadCornerRadii(0, 6, 10)`. Radii are ordered; zero means square. Borders are positive and ordered, with equal roles allowed; the 1/2 dp defaults are not pixel hairlines. Example: `MadDivider(borderWidths = MadBorderWidths(2, 3), role = MadBorderRole.Strong)`. The decorative divider fills bounded available width; supply a width in an unbounded parent. A tight height clips its artwork to the measured height. Desktop Skia artwork checks do not qualify native Android, iOS or HarmonyOS. Existing controls do not automatically take these roles, and the full state matrix remains pending.

## Radio group

Mark the group with `Modifier.selectableGroup()` and keep the active choice in one host-owned value. Only an enabled, unselected row invokes `onSelect`; selected and disabled rows do not. Supply nonblank localized labels. The row offers at least 48 × 48 dp when constraints allow. Desktop Compose semantics and touch do not establish native accessibility or device behavior.

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

`MadCheckbox(label = "Save for offline reading", checked = saved, onCheckedChange = { saved = it })` changes selection only when the host updates `checked`. It does not implement tri-state or mutually exclusive selection. Supply a localized nonblank label and do not add a competing name or action through the modifier. The full row is at least 48 × 48 dp when constraints allow. Disabled never calls back. The Desktop smoke exercises Compose semantics and touch; it does not establish TalkBack, VoiceOver, Android, iOS, or HarmonyOS behavior.

## Reproducible local build, when approved

- Use JDK 17, the checked-in Gradle 8.13 wrapper/checksum, Android SDK Platform 36, Kotlin/Compose compiler 2.2.21, CMP 1.9.3, AGP 8.11.1. For Apple leaves use a qualified macOS/Xcode host and record actual Xcode and SDK. Do not mix stages across hosts or earlier versions.
- Fresh Gradle home per publisher and consumer, with approved official dependencies; use an explicit new empty filesystem stage outside the repo. Never use the default publishing directory in this rehearsal. Run source verification first: `python3 scripts/verify-preview-source.py`.
- From the SDK checkout, publish only to the isolated filesystem Maven stage on the approved host:

```sh
set -eu
preview_work="$(mktemp -d "${TMPDIR:-/tmp}/mad-alpha18-sdk-preview01.XXXXXX")"
stage="$preview_work/maven"
publisher_home="$preview_work/gradle-publisher"
consumer_home="$preview_work/gradle-consumer-components"
mkdir "$stage" "$publisher_home" "$consumer_home"
test -z "$(ls -A "$stage")"
python3 scripts/verify-preview-source.py
GRADLE_USER_HOME="$publisher_home" ./gradlew --no-daemon --console=plain "-PmaduikitStagingDir=$stage" \
  :maduikit-theme:publishKotlinMultiplatformPublicationToMadUIKitLocalRepository \
  :maduikit-theme:publishAndroidReleasePublicationToMadUIKitLocalRepository \
  :maduikit-components:publishKotlinMultiplatformPublicationToMadUIKitLocalRepository \
  :maduikit-components:publishAndroidReleasePublicationToMadUIKitLocalRepository
```

A single qualified macOS producer run for Android + Apple would include each module's `publishIosSimulatorArm64PublicationToMadUIKitLocalRepository` and `publishIosArm64PublicationToMadUIKitLocalRepository` **in that same command and stage**. Do not add leaves later to an accepted stage. Record source commit, file manifest, wrapper SHA, versions, exact output hashes, SDK root metadata redirect/leaf edges, and full stage hashes before/after external consumption; reproducibility means pinned inputs and repeatable checks, not a byte-identical build promise. Stage equality is required during a test, not claimed between independent jobs.

### Independent Android consumer

The following is a continuation of the same shell session, not a standalone
script. Start with an existing Android CMP/Compose consumer and its own checked-in
wrapper. In its `settings.gradle.kts`, add this block inside
`dependencyResolutionManagement.repositories`; leave its other official repositories in place:

```kotlin
exclusiveContent {
    forRepository {
        maven {
            name = "madUIKitPreview"
            url = uri(providers.gradleProperty("madPreviewStage").get())
        }
    }
    filter { includeGroup("com.mad.maduikit") }
}
```

In an Android-only consumer add this exact dependency in the application
module (in `androidMain.dependencies` for an Android-only stage used by a KMP app):

```kotlin
implementation("com.mad.maduikit:maduikit-components:0.1.0-alpha18-sdk-preview01")
```

Run from that independent consumer checkout, replacing `:app` with its module
name if necessary. The explicit AndroidX opt-in applies to the consumer and
does not depend on the producer's Gradle properties.

```sh
cd /absolute/path/to/independent-android-consumer
GRADLE_USER_HOME="$consumer_home" ./gradlew --no-daemon --console=plain \
  "-PmadPreviewStage=$stage" -Pandroid.useAndroidX=true :app:assembleDebug
```

For the separate theme-only arm use a fresh Gradle home created with
`mkdir "$preview_work/gradle-consumer-theme"`; change the consumer root
coordinate to `maduikit-theme` at the same preview version. An Android/iOS
shared `commonMain` dependency needs the verified corresponding target leaves.
Do not attempt to resolve iOS from the Android-only stage above.

A standalone consumer should declare only the exact Maven root in each arm: theme-only and components-only (no direct theme in the latter). Resolve actual Android API/runtime and each claimed Native target separately. Confirm raw stage artifacts against the manifest, selected variants, compiler inputs and old caller compile. Add native behavior checks separately. The source-only verifier and offline Compose probe do not prove these packaging/runtime gates.

## Rollback

Keep each accepted stage archive immutable and separate. Revert dependency version **and** exclusive stage path to the previously verified pair, clear isolated project/Gradle homes as planned, and repeat the same caller/provenance checks. No patch to old SDK consumers or old artifacts occurred here. This describes a rollback recipe, not an exercised A/B/A rollback.

## MadSwitch local Desktop contract

The host owns checked and handles onCheckedChange requests. A nonblank localized label is required. The label and decorative Material switch share one semantic switch action. Long labels may wrap. No new theme tokens or SDK dependencies are added.

Use the pinned wrapper from the root; the smoke project is separate from the SDK publication:

    ./gradlew -p smoke/switch-desktop run
    ./gradlew -p smoke/switch-desktop run --args=long

The roleless and blank smoke arguments are deliberately failing controls. These tests exercise Compose Desktop semantics and pointer input, not Android, iOS, HarmonyOS, or their platform accessibility services. This source review does not ship a staged package.

## MadIcon and MadIconButton Desktop contract

MadIcon displays the host Painter. A null content description is decorative; a nonblank localized description names a meaningful non-action image. Unspecified tint preserves original painter colors. MadIconButton requires a nonblank localized action label and exposes one action, with a decorative child and a 48dp minimum target where parent constraints allow. The host owns actions and enabled state. No icon assets, tokens or SDK dependencies were added.

    ./gradlew -p smoke/icon-desktop run

Deliberately failing smoke arguments: blank-icon, blank-action, duplicate-label. Desktop proof binds the actual Compose semantics, density/target and pointer behavior, not a native platform accessibility result.
