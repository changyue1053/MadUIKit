"""Check the reviewed source contract; does not invoke Gradle or test publications."""
from pathlib import Path
import hashlib
import re

ROOT = Path(__file__).resolve().parents[1]
VER = "0.1.0-alpha06-sdk-preview01"
PKG = "src/commonMain/kotlin/com/mad/maduikit"
FILES = {
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
    print("PASS: source pins, exact six payloads, switch and icon probes, successor coordinate and two root publication wiring checks")


if __name__ == "__main__":
    verify(ROOT)
