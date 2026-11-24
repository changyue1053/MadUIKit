"""Check the reviewed source contract; does not invoke Gradle or test publications."""
from pathlib import Path
import hashlib
import re

ROOT = Path(__file__).resolve().parents[1]
VER = "0.1.0-alpha03-sdk-preview01"
PKG = "src/commonMain/kotlin/com/mad/maduikit"
FILES = {
    ("theme", "MadSpacing"): "82f685cd96692702b9384d5f0091788739092cdb2678f2889dc55920abd7ec1d",
    ("theme", "MadTypography"): "99a0265444a19ae27da16450ba64343a67c8e8c415be4e20f3a55973cd4ac777",
    ("theme", "MadProvideTypography"): "25d57e1e48f1b494e872cc03593d83c7eb9e5337d1c07d8d7d81b33f92feb5c0",
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
    print("PASS: source pins, exact four payloads, preview coordinate and two root publication wiring checks")


if __name__ == "__main__":
    verify(ROOT)
