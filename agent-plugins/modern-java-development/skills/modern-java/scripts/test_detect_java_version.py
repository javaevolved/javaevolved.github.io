import os
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parent))

import detect_java_version as detector


class DetectJavaVersionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    def detect(self, explicit=None):
        with (
            patch.object(detector, "runtime_version", return_value=("25", "java on PATH")),
            patch.dict(os.environ, {"JAVA_VERSION": ""}, clear=False),
        ):
            return detector.detect(self.root, explicit, 3)

    def test_maven_release_resolves_property_and_beats_runtime(self):
        (self.root / "pom.xml").write_text(
            """
            <project>
              <properties><java.version>17</java.version></properties>
              <build><plugins><plugin><configuration>
                <release>${java.version}</release>
              </configuration></plugin></plugins></build>
            </project>
            """,
            encoding="utf-8",
        )

        result = self.detect()

        self.assertEqual(17, result["selected"]["version"])
        self.assertEqual("maven-release", result["selected"]["source"])
        self.assertFalse(result["ambiguous"])

    def test_gradle_release_beats_toolchain(self):
        (self.root / "build.gradle.kts").write_text(
            """
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
            tasks.withType<JavaCompile> { options.release.set(17) }
            """,
            encoding="utf-8",
        )

        result = self.detect()

        self.assertEqual(17, result["selected"]["version"])
        self.assertEqual("gradle-release", result["selected"]["source"])

    def test_maven_compiler_toolchain_is_detected(self):
        (self.root / "pom.xml").write_text(
            """
            <project><build><plugins><plugin><configuration>
              <jdkToolchain><version>21</version></jdkToolchain>
            </configuration></plugin></plugins></build></project>
            """,
            encoding="utf-8",
        )

        result = self.detect()

        self.assertEqual(21, result["selected"]["version"])
        self.assertEqual("maven-toolchain", result["selected"]["source"])

    def test_version_manager_beats_runtime(self):
        (self.root / ".java-version").write_text("temurin-21.0.4\n", encoding="utf-8")

        result = self.detect()

        self.assertEqual(21, result["selected"]["version"])
        self.assertEqual("java-version-file", result["selected"]["source"])

    def test_explicit_version_has_highest_precedence(self):
        (self.root / "pom.xml").write_text(
            "<project><properties><maven.compiler.release>17"
            "</maven.compiler.release></properties></project>",
            encoding="utf-8",
        )

        result = self.detect("11")

        self.assertEqual(11, result["selected"]["version"])
        self.assertEqual("explicit", result["selected"]["source"])

    def test_conflicting_module_releases_are_ambiguous(self):
        for module, version in (("api", 17), ("app", 21)):
            directory = self.root / module
            directory.mkdir()
            (directory / "pom.xml").write_text(
                f"<project><properties><maven.compiler.release>{version}"
                "</maven.compiler.release></properties></project>",
                encoding="utf-8",
            )

        result = self.detect()

        self.assertTrue(result["ambiguous"])
        self.assertEqual({17, 21}, {
            item["version"]
            for item in result["candidates"]
            if item["source"] == "maven-release"
        })

    def test_legacy_java_version_is_normalized(self):
        self.assertEqual(8, detector.normalize_version("1.8.0_402"))


if __name__ == "__main__":
    unittest.main()
