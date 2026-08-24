import re
import unittest
from pathlib import Path


class ReferenceCoverageTests(unittest.TestCase):
    def test_every_catalog_pattern_is_covered_by_reference_guidance(self):
        repository = Path(__file__).resolve().parents[5]
        content = repository / "content"
        if not content.is_dir():
            self.skipTest("java.evolved catalog is not present in packaged plugin")

        references = Path(__file__).resolve().parents[1] / "references"
        covered = set()
        for path in references.glob("*.md"):
            for marker in re.findall(r"<!--\s*covers:\s*([^>]+)-->", path.read_text()):
                covered.update(marker.split())

        patterns = {
            path.stem
            for path in content.glob("*/*.yaml")
            if path.name != "template.yaml"
        }
        self.assertEqual(set(), patterns - covered)


if __name__ == "__main__":
    unittest.main()
