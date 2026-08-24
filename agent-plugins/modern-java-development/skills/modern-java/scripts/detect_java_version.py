#!/usr/bin/env python3
"""Detect a Java project's effective compilation target."""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from dataclasses import asdict, dataclass
from pathlib import Path


IGNORED_DIRS = {
    ".git",
    ".gradle",
    ".idea",
    ".mvn",
    ".vscode",
    "build",
    "node_modules",
    "out",
    "target",
}

SOURCE_PRIORITY = {
    "explicit": 100,
    "maven-release": 90,
    "gradle-release": 90,
    "maven-toolchain": 80,
    "gradle-toolchain": 80,
    "maven-source": 70,
    "gradle-source": 70,
    "java-version-file": 60,
    "sdkman": 60,
    "asdf": 60,
    "ci": 50,
    "environment": 40,
    "runtime": 10,
}


@dataclass(frozen=True)
class Candidate:
    version: int
    source: str
    location: str
    raw: str
    priority: int


def normalize_version(value: str | int | None) -> int | None:
    if value is None:
        return None
    text = str(value).strip().strip("\"'")
    match = re.search(r"(?<!\d)(?:1\.)?(\d{1,3})(?:[._+-]\d+)*", text)
    if not match:
        return None
    version = int(match.group(1))
    if text.startswith("1.") and version == 1:
        legacy = re.match(r"1\.(\d+)", text)
        return int(legacy.group(1)) if legacy else None
    return version if version >= 5 else None


def add_candidate(
    candidates: list[Candidate],
    value: str | int | None,
    source: str,
    location: Path | str,
) -> None:
    version = normalize_version(value)
    if version is not None:
        candidates.append(
            Candidate(
                version=version,
                source=source,
                location=str(location),
                raw=str(value).strip(),
                priority=SOURCE_PRIORITY[source],
            )
        )


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def resolve_maven_value(value: str, properties: dict[str, str]) -> str:
    seen: set[str] = set()
    current = value.strip()
    while True:
        match = re.fullmatch(r"\$\{([^}]+)}", current)
        if not match or match.group(1) in seen:
            return current
        key = match.group(1)
        seen.add(key)
        current = properties.get(key, current).strip()


def inspect_maven(path: Path, candidates: list[Candidate]) -> None:
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError):
        return

    properties: dict[str, str] = {}
    for element in root.iter():
        if local_name(element.tag) == "properties":
            for child in element:
                if child.text:
                    properties[local_name(child.tag)] = child.text.strip()

    release_names = {"maven.compiler.release", "release"}
    source_names = {"maven.compiler.source", "source"}
    for element in root.iter():
        name = local_name(element.tag)
        if name == "jdkToolchain":
            for child in element.iter():
                if local_name(child.tag) == "version" and child.text:
                    add_candidate(
                        candidates,
                        resolve_maven_value(child.text, properties),
                        "maven-toolchain",
                        path,
                    )
            continue
        if not element.text:
            continue
        value = resolve_maven_value(element.text, properties)
        if name in release_names:
            add_candidate(candidates, value, "maven-release", path)
        elif name in source_names:
            add_candidate(candidates, value, "maven-source", path)

    for key in ("java.version", "jdk.version"):
        if key in properties:
            add_candidate(candidates, properties[key], "maven-source", path)


def inspect_gradle(path: Path, candidates: list[Candidate]) -> None:
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError):
        return

    patterns = [
        (
            "gradle-release",
            r"(?:options\.)?release(?:\.set)?\s*\(?\s*(\d{1,3})",
        ),
        (
            "gradle-toolchain",
            r"JavaLanguageVersion\.of\s*\(\s*(\d{1,3})\s*\)",
        ),
        (
            "gradle-source",
            r"(?:sourceCompatibility|targetCompatibility)\s*=\s*"
            r"(?:JavaVersion\.VERSION_)?[\"']?(?:1[_.])?(\d{1,3})",
        ),
    ]
    for source, pattern in patterns:
        for match in re.finditer(pattern, text):
            add_candidate(candidates, match.group(1), source, path)


def inspect_version_files(root: Path, candidates: list[Candidate]) -> None:
    files = [
        (".java-version", "java-version-file"),
        (".sdkmanrc", "sdkman"),
        (".tool-versions", "asdf"),
    ]
    for filename, source in files:
        path = root / filename
        if not path.is_file():
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        if filename == ".sdkmanrc":
            match = re.search(r"(?m)^\s*java\s*=\s*(\S+)", text)
            value = match.group(1) if match else None
        elif filename == ".tool-versions":
            match = re.search(r"(?m)^\s*java\s+(\S+)", text)
            value = match.group(1) if match else None
        else:
            value = text.splitlines()[0] if text.splitlines() else None
        add_candidate(candidates, value, source, path)


def inspect_ci(root: Path, candidates: list[Candidate]) -> None:
    workflows = root / ".github" / "workflows"
    if not workflows.is_dir():
        return
    for path in sorted((*workflows.glob("*.yml"), *workflows.glob("*.yaml"))):
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        for match in re.finditer(
            r"(?m)^\s*java-version\s*:\s*[\"']?([^\"'\s#]+)", text
        ):
            add_candidate(candidates, match.group(1), "ci", path)


def build_files(root: Path, max_depth: int) -> list[Path]:
    result: list[Path] = []
    for current, dirs, files in os.walk(root):
        current_path = Path(current)
        depth = len(current_path.relative_to(root).parts)
        dirs[:] = [
            item
            for item in dirs
            if item not in IGNORED_DIRS and depth < max_depth
        ]
        for filename in files:
            if filename in {"pom.xml", "build.gradle", "build.gradle.kts"}:
                result.append(current_path / filename)
    return sorted(result)


def runtime_version() -> tuple[str | None, str | None]:
    try:
        process = subprocess.run(
            ["java", "-XshowSettings:properties", "-version"],
            capture_output=True,
            text=True,
            timeout=5,
            check=False,
        )
    except (OSError, subprocess.SubprocessError):
        return None, None
    output = process.stdout + process.stderr
    match = re.search(r"java\.specification\.version\s*=\s*(\S+)", output)
    return (match.group(1), "java on PATH") if match else (None, None)


def detect(root: Path, explicit: str | None, max_depth: int) -> dict[str, object]:
    candidates: list[Candidate] = []
    if explicit:
        add_candidate(candidates, explicit, "explicit", "command line")

    for path in build_files(root, max_depth):
        if path.name == "pom.xml":
            inspect_maven(path, candidates)
        else:
            inspect_gradle(path, candidates)

    inspect_version_files(root, candidates)
    inspect_ci(root, candidates)
    add_candidate(candidates, os.environ.get("JAVA_VERSION"), "environment", "JAVA_VERSION")
    value, location = runtime_version()
    add_candidate(candidates, value, "runtime", location or "java on PATH")

    unique = list(
        {
            (item.version, item.source, item.location, item.raw): item
            for item in candidates
        }.values()
    )
    ordered = sorted(
        unique,
        key=lambda item: (-item.priority, item.location, item.version),
    )
    selected = ordered[0] if ordered else None
    strongest = [item for item in ordered if selected and item.priority == selected.priority]
    ambiguous = len({item.version for item in strongest}) > 1
    conflicts = [
        item
        for item in ordered
        if selected and item.version != selected.version and item.priority >= 70
    ]

    return {
        "root": str(root),
        "selected": asdict(selected) if selected else None,
        "ambiguous": ambiguous,
        "conflicts": [asdict(item) for item in conflicts],
        "candidates": [asdict(item) for item in ordered],
    }


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Detect the effective Java compilation target for a project."
    )
    parser.add_argument("path", nargs="?", default=".", help="project root")
    parser.add_argument(
        "--java-version",
        help="explicit target supplied by the user (highest precedence)",
    )
    parser.add_argument(
        "--max-depth",
        type=int,
        default=3,
        help="maximum build-file search depth (default: 3)",
    )
    args = parser.parse_args()

    root = Path(args.path).expanduser().resolve()
    if not root.is_dir():
        print(json.dumps({"error": f"not a directory: {root}"}), file=sys.stderr)
        return 2
    if args.java_version and normalize_version(args.java_version) is None:
        print(
            json.dumps({"error": f"invalid Java version: {args.java_version}"}),
            file=sys.stderr,
        )
        return 2

    result = detect(root, args.java_version, max(0, args.max_depth))
    print(json.dumps(result, indent=2))
    return 0 if result["selected"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
