# Source-native S1 developer preview

Source baseline: `49f2c1f07bccaf2036ed5bdafe53d6412da45668`. This source-only successor adds controlled binary selection via `MadCheckbox` and leaves previous versions, including alpha06, unchanged. The reserved coordinate is `0.1.0-alpha07-sdk-preview01`; no Maven bundle has been published.

`MadCheckbox(label = "Save for offline reading", checked = saved, onCheckedChange = { saved = it })` changes selection only when the host updates `checked`. It does not implement tri-state or mutually exclusive selection. Supply a localized nonblank label and do not add a competing name or action through the modifier. The full row is at least 48 × 48 dp when constraints allow. Disabled never calls back. The Desktop smoke exercises Compose semantics and touch; it does not establish TalkBack, VoiceOver, Android, iOS, or HarmonyOS behavior.

## Reproducible local build, when approved

- Use JDK 17, the checked-in Gradle 8.13 wrapper/checksum, Android SDK Platform 36, Kotlin/Compose compiler 2.2.21, CMP 1.9.3, AGP 8.11.1. For Apple leaves use a qualified macOS/Xcode host and record actual Xcode and SDK. Do not mix stages across hosts or earlier versions.
- Fresh Gradle home per publisher and consumer, with approved official dependencies; use an explicit new empty filesystem stage outside the repo. Never use the default publishing directory in this rehearsal. Run source verification first: `python3 scripts/verify-preview-source.py`.
- From the SDK checkout, publish only to the isolated filesystem Maven stage on the approved host:

```sh
set -eu
preview_work="$(mktemp -d "${TMPDIR:-/tmp}/mad-alpha07-sdk-preview01.XXXXXX")"
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
implementation("com.mad.maduikit:maduikit-components:0.1.0-alpha07-sdk-preview01")
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
