"""Check the reviewed source contract; does not invoke Gradle or test publications."""
from pathlib import Path
import hashlib
import re

ROOT = Path(__file__).resolve().parents[1]
VER = "0.1.0-alpha27-sdk-preview01"
PKG = "src/commonMain/kotlin/com/mad/maduikit"
FILES = {
    ("components", "MadScaffold"): "eb7afc4bd7c010ea8bb313c78dec5ca800edbdc8ad35008bc2bed6ee74894f18",
    ("components", "MadSearchFieldWithClear"): "df27be6e443bef942199fe0a6f79ad6f4e94dd286b6a50caa2528e9c08ed3e93",
    ("components", "MadDecoratedTextField"): "72bb2bd13fe33b66298c13cc00354288bf4c265677b50c98289d2650d15fef81",
    ("components", "MadTextArea"): "5435a1c9874e965bdcc502adde0c1c107b1119f3e62249e919ac505e90a07737",
    ("components", "MadBottomSheet"): "9428dcd9107e1f986d973981327b0de7760856a49a60ed7a3768263bbb212af9",
    ("components", "MadBanner"): "92e9b7b2f7b870104f7ccd212f48f9ff99d6a64c5fca36064bfa979ceda2f93a",
    ("components", "MadStatePanel"): "1675c9d1f9089144ca91941ea5f425097fe7e04ca0c60073fd409dc8dd2ac347",
    ("components", "MadTopAppBar"): "98c869911d3f9f118cfc9ddee1f1058b2e9c51986aefa43e4645c629c466d427",
    ("components", "MadConfirmDialog"): "515e35031a04c3328db73b756ed652735f4d6cb291704bc05ec17873e4f3d3ff",
    ("components", "MadLinearProgress"): "553a58ed22e4484486b2617e86b0946e06e44fe1c9ebfdcd69a114ded3e39c2c",
    ("components", "MadNavigationBar"): "529ddeb39dc28df32efc08645f2e44800be2571980214e2fbd9d69f57e900cd6",
    ("components", "MadSearchField"): "5a1aae3297705fee552c8fbc75a854a1a613011d17a87fbe14faef147c8ac1e1",
    ("components", "MadAvatar"): "fb9522aa6711b487b543a643c162451984f53bbc9d4a7f676b4e7d8d76434bae",
    ("components", "MadCard"): "9aeb2bc8ce46352a97a79057c2cd8aedc83ed3db7a90166ec338f43734067983",
    ("components", "MadListItem"): "2b6363065742ba9ffebab2e76a029cf65f0b8100de2434a138768a4177002d87",
    ("components", "MadTextField"): "0b64983dcd5d057014e84448932105e61e58b637bfdac4dfe3134546db5f51e1",
    ("components", "MadBadge"): "7cdf29efadf616a744b6ca9a8ce6c3ab6daa5173dc324ae4d1ab2f2ba165dd66",
    ("components", "MadButton"): "7d849b90677a58443f46ea6b62b31205f71d832988a979e9e2cf4d2de1db4cf0",
    ("components", "MadActionButton"): "8211be630f24a9abe0ce5443fee71ef389a048c12ba10f00444c73863fddb560",
    ("components", "MadDivider"): "d9f9fda12347066f610b0cef23001cf007114bf3ca1ae4e5bbbff3120ec6a033",
    ("theme", "MadGeometryTokens"): "4b2adadeec59615709220c15dcbc8f0c062a5dbbeb2f5fa97c926878a58daebf",
    ("components", "MadRadio"): "9f5f82989b8d757ee658c7b25931ac0ae93daecebfd55319db6983700e2b9e15",
    ("components", "MadCheckbox"): "7980507ad5b7841ce7f8121d3807084f22798fbd6d57b26341c5bc613c17e2ac",
    ("theme", "MadSpacing"): "82f685cd96692702b9384d5f0091788739092cdb2678f2889dc55920abd7ec1d",
    ("theme", "MadTypography"): "99a0265444a19ae27da16450ba64343a67c8e8c415be4e20f3a55973cd4ac777",
    ("theme", "MadProvideTypography"): "25d57e1e48f1b494e872cc03593d83c7eb9e5337d1c07d8d7d81b33f92feb5c0",
    ("components", "MadIcon"): "66b06cf2171c20694675ae11c455ca3a05eff5070a31c5e3202d0e81c2cef737",
    ("components", "MadSwitch"): "07fafcfb0c020779ac7667fecc9a1da0d07b6c3452372d32aea6da05ade2ee20",
    ("components", "MadText"): "78525f3473e220c69c04d01886f149b89aa1e485691f8e92ec1bf687291d14e4",
}


def check(ok, reason):
    if not ok:
        raise SystemExit(reason)


def verify(root):
    def script(path):
        text = (root / path).read_text()
        return re.sub(r"(?m)//.*$", "", text)
    settings = script("settings.gradle.kts")
    check('include(":maduikit-theme", ":maduikit-components")' in settings, "wrong root modules")
    config = script("build.gradle.kts")
    check(re.findall(r'(?m)^\s*version = "([^"]+)"$', config) == [VER], "wrong preview coordinate")
    check('group = "com.mad.maduikit"' in config, "wrong Maven group")
    for plugin in ('kotlin("multiplatform") version "2.2.21"',
                   'id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"',
                   'id("org.jetbrains.compose") version "1.9.3"',
                   'id("com.android.library") version "8.11.1"'):
        check(plugin in config, "wrong pinned plugin")
    for m in ("theme", "components"):
        block = script(f"maduikit-{m}/build.gradle.kts")
        for token in ('api(compose.runtime)', 'api(compose.ui)', 'iosArm64()', 'iosSimulatorArm64()',
                      'publishLibraryVariants("release")', 'jvmToolchain(17)', 'id("org.jetbrains.kotlin.plugin.compose")',
                      'compileSdk = 36', 'minSdk = 23', '`maven-publish`', 'name = "madUIKitLocal"',
                      'gradleProperty("maduikitStagingDir")', '.toURI()'):
            check(token in block, f"{m}: missing source wiring {token}")
        check('implementation("org.jetbrains.compose.material3:material3:1.9.0")' in block,
              f"{m}: Material3 implementation boundary")
        check(not re.search(r"api\([^\n]*material3", block, re.I), f"{m}: Material3 must not be exported")
        if m == "components":
            check('api(project(":maduikit-theme"))' in block and 'api(compose.foundation)' in block,
                  "components must export theme and Foundation layout API")
    for (module, name), digest in FILES.items():
        data = (root / f"maduikit-{module}" / PKG / f"{name}.kt").read_bytes()
        check(hashlib.sha256(data).hexdigest() == digest, f"{name}: reviewed source hash mismatch")
        check(b"package com.mad.maduikit" in data, f"{name}: wrong public Kotlin package")
    smoke = root / "smoke/switch-desktop/src/main/kotlin/SwitchProbe.kt"
    check(hashlib.sha256(smoke.read_bytes()).hexdigest() == "100820d5cc1c43bad390a134fae7b8a5f55eff0870e5e6acf7f633d3bbb29788",
          "SwitchProbe: reviewed test hash mismatch")
    smoke_icon = root / "smoke/icon-desktop/src/main/kotlin/IconProbe.kt"
    check(hashlib.sha256(smoke_icon.read_bytes()).hexdigest() == "6ac5819ef1414f1d768605ccdcb3047746b89a7275541aef6b3a43a6c9791c6f",
          "IconProbe: reviewed test hash mismatch")
    smoke_checkbox = root / "smoke/checkbox-desktop/src/main/kotlin/CheckboxProbe.kt"
    check(hashlib.sha256(smoke_checkbox.read_bytes()).hexdigest() == "cd3707a950b4399334614e6e42f3de08e3e5d0dbdf31e21d193e7ebf585cc41a",
          "CheckboxProbe: reviewed test hash mismatch")
    smoke_radio = root / "smoke/radio-desktop/src/main/kotlin/RadioProbe.kt"
    check(hashlib.sha256(smoke_radio.read_bytes()).hexdigest() == "56e7375706c814b1daca142aac71a10113a01f2213a9c609253cc729a37ec1e0",
          "RadioProbe: reviewed test hash mismatch")
    smoke_geometry = root / "smoke/geometry-desktop/src/main/kotlin/GeometryProbe.kt"
    check(hashlib.sha256(smoke_geometry.read_bytes()).hexdigest() == "22b80e1074179cc05373447201fa0288169e6690ba1d9c55f1cbc3d2853bf99e",
          "GeometryProbe: reviewed test hash mismatch")
    smoke_button_family = root / "smoke/button-family-desktop/src/main/kotlin/ButtonFamilyProbe.kt"
    check(hashlib.sha256(smoke_button_family.read_bytes()).hexdigest() == "edbb474fc0ea3b3504f3a9e97e1e24ea0be6a4d32c15fdce31d96a1cba8c67d2",
          "ButtonFamilyProbe: reviewed test hash mismatch")
    smoke_badge = root / "smoke/badge-desktop/src/main/kotlin/BadgeProbe.kt"
    check(hashlib.sha256(smoke_badge.read_bytes()).hexdigest() == "ad2639f2c879368e3fda7a5f3bd3501dd67eaaff695c71218f96055646bd1a96",
          "BadgeProbe: reviewed test hash mismatch")
    smoke_badge_shape = root / "smoke/badge-desktop/src/main/kotlin/ShapeAudit.kt"
    check(hashlib.sha256(smoke_badge_shape.read_bytes()).hexdigest() == "2c804401dde8333dd8fd9429bdc7e7dc0d998d4c27d92a21760f328f1ca0d360",
          "ShapeAudit: reviewed test hash mismatch")
    smoke_textfield = root / "smoke/textfield-desktop/src/main/kotlin/FormReplay.kt"
    check(hashlib.sha256(smoke_textfield.read_bytes()).hexdigest() == "de9c773c2375d0d4a6c63107665c239eba5c16b3dacf9328bf3df006778ad30b",
          "FormReplay: reviewed test hash mismatch")
    smoke_list_item = root / "smoke/list-item-desktop/src/main/kotlin/ListItemProbe.kt"
    check(hashlib.sha256(smoke_list_item.read_bytes()).hexdigest() == "19447e66d9220abbc0e2c6554f3bb4ad36c9a82843b77414cfa3e0688ee4e5a3",
          "ListItemProbe: reviewed test hash mismatch")
    smoke_card = root / "smoke/card-desktop/src/main/kotlin/CardProbe.kt"
    check(hashlib.sha256(smoke_card.read_bytes()).hexdigest() == "4c7aaf38bd9f05e074f8eaf7b9cdbb07ea870af2519fe0abd88d6e3e44a1588c",
          "CardProbe: reviewed test hash mismatch")
    smoke_avatar = root / "smoke/avatar-desktop/src/main/kotlin/AvatarProbe.kt"
    check(hashlib.sha256(smoke_avatar.read_bytes()).hexdigest() == "fbf667a30f99b78d6e9f3c22ae3643579b0d6ff1d410d513d98f0627dcee1cac",
          "AvatarProbe: reviewed test hash mismatch")
    smoke_search = root / "smoke/search-field-desktop/src/main/kotlin/SearchReplay.kt"
    check(hashlib.sha256(smoke_search.read_bytes()).hexdigest() == "3c69b5dc377d8661aa9d2506af3084e8a849d4321b0c75297c3078c571a9677b",
          "SearchReplay: reviewed test hash mismatch")
    smoke_navigation = root / "smoke/navigation-desktop/src/main/kotlin/NavigationProbe.kt"
    check(hashlib.sha256(smoke_navigation.read_bytes()).hexdigest() == "c4b428b105d02a066a14c198c446fe118d6b1fb3e53845194faa5b1a7ce2afeb",
          "NavigationProbe: reviewed test hash mismatch")
    smoke_progress = root / "smoke/progress-desktop/src/main/kotlin/ProgressProbe.kt"
    check(hashlib.sha256(smoke_progress.read_bytes()).hexdigest() == "579df3b5746b42b73e6eed896f865eb5c1b95cb3cc7a4e98e4ed50cf256a5a93",
          "ProgressProbe: reviewed test hash mismatch")
    smoke_confirm = root / "smoke/confirm-dialog-desktop/src/main/kotlin/ConfirmProbe.kt"
    check(hashlib.sha256(smoke_confirm.read_bytes()).hexdigest() == "7721ecf5cd24399709990a8fde8cbe4d59bc8183b6267aad23a6bdae2231843a",
          "ConfirmProbe: reviewed test hash mismatch")
    smoke_top_bar = root / "smoke/top-bar-desktop/src/main/kotlin/TopBarProbe.kt"
    check(hashlib.sha256(smoke_top_bar.read_bytes()).hexdigest() == "d4f511348fcc7b99dcf956c83329592ddfbe2694d1a313d3decfad391cb2bf2b",
          "TopBarProbe: reviewed test hash mismatch")
    smoke_panel = root / "smoke/state-panel-desktop/src/main/kotlin/StatePanelProbe.kt"
    check(hashlib.sha256(smoke_panel.read_bytes()).hexdigest() == "2a761752a8dc328a7fe91060be4b322139e1a239e70e9350c3f200ce3f843683",
          "StatePanelProbe: reviewed test hash mismatch")
    smoke_banner = root / "smoke/banner-desktop/src/main/kotlin/BannerProbe.kt"
    check(hashlib.sha256(smoke_banner.read_bytes()).hexdigest() == "6c11a16e3e11bee57bc693565253b7b8db56e05485020bc1f6f1f4566400d5d0",
          "BannerProbe: reviewed test hash mismatch")
    smoke_sheet = root / "smoke/bottom-sheet-desktop/src/main/kotlin/BottomSheetProbe.kt"
    check(hashlib.sha256(smoke_sheet.read_bytes()).hexdigest() == "70f74b7e4d082adbccc05fddeb77f2f9691c662c637ea6632a9a243833753888",
          "BottomSheetProbe: reviewed test hash mismatch")
    smoke_area = root / "smoke/text-area-desktop/src/main/kotlin/TextAreaProbe.kt"
    check(hashlib.sha256(smoke_area.read_bytes()).hexdigest() == "748328499ec6a0423a7ac149c40334bfe935772f73d7c50f0241ed45a4f98179",
          "TextAreaProbe: reviewed test hash mismatch")
    smoke_decorated = root / "smoke/decorated-field-desktop/src/main/kotlin/DecoratedProbe.kt"
    check(hashlib.sha256(smoke_decorated.read_bytes()).hexdigest() == "f48e1b8d4d71c241717d15d3d2bdc77dd6b436bd0f23143719c20af046f70ea3",
          "DecoratedProbe: reviewed test hash mismatch")
    smoke_clear = root / "smoke/search-clear-desktop/src/main/kotlin/ClearProbe.kt"
    check(hashlib.sha256(smoke_clear.read_bytes()).hexdigest() == "0509c19d18d7e1139d3a21e38f05775f412f997838f7e22ddd18c6663bf51922",
          "ClearProbe: reviewed test hash mismatch")
    smoke_scaffold = root / "smoke/scaffold-desktop/src/main/kotlin/ScaffoldProbe.kt"
    check(hashlib.sha256(smoke_scaffold.read_bytes()).hexdigest() == "264c9ea92c15c08f5239b8b09a7a348d56de42ea362440abe82d6adc395f978e",
          "ScaffoldProbe: reviewed test hash mismatch")
    print("PASS: source pins, exact twenty-nine payloads and twenty-three Desktop probes, successor coordinate and two root publication wiring checks")


if __name__ == "__main__":
    verify(ROOT)
