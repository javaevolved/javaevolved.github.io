# Modern Java Development agent plugin

A portable [Agent Plugins 1.0](https://agent-plugins.org/specification) package
for version-aware Java development.

The plugin contributes the `modern-java` skill. The skill detects the project's
effective Java compilation target before recommending language features, APIs,
tooling, or modernization changes. Guidance is cumulative and grouped by the
Java release in which each capability became final.

## Package layout

```text
modern-java-development/
├── plugin.json
└── skills/
    └── modern-java/
        ├── SKILL.md
        ├── references/
        │   ├── core-practices.md
        │   └── release-practices.md
        └── scripts/
            ├── detect_java_version.py
            └── test_detect_java_version.py
```

## Use

Install or copy this directory into any Agent Plugins-compatible client. When
the skill is active, the agent runs the detector from the Java project root:

```bash
python3 skills/modern-java/scripts/detect_java_version.py .
```

Pass an explicit target when build metadata is unavailable:

```bash
python3 skills/modern-java/scripts/detect_java_version.py . --java-version 21
```

The detector emits JSON so an agent can distinguish the selected target from
lower-confidence runtime evidence and report conflicting build configuration.

## Validate

```bash
python3 -m unittest \
  skills/modern-java/scripts/test_detect_java_version.py
```
