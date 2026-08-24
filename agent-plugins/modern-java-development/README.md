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
        │   ├── enterprise-practices.md
        │   └── release-practices.md
        └── scripts/
            ├── detect_java_version.py
            ├── test_reference_coverage.py
            └── test_detect_java_version.py
```

## Install

Versioned `.tar.gz` packages and SHA-256 checksums are available from the
[GitHub Releases](https://github.com/javaevolved/javaevolved.github.io/releases).
Each archive contains the `modern-java-development` package directory.

### GitHub Copilot CLI

GitHub Copilot CLI supports the Agent Plugins specification. Add the
java.evolved marketplace once:

```bash
copilot plugin marketplace add javaevolved/javaevolved.github.io
```

Then install the plugin:

```bash
copilot plugin install modern-java-development@javaevolved
```

See the
[GitHub Copilot CLI plugin reference](https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-plugin-reference)
for marketplace management and plugin updates.

### Claude Code

Claude Code supports the Agent Skills format used by the `modern-java` skill.
Clone this repository and copy the skill to your personal skills directory:

```bash
git clone --depth 1 https://github.com/javaevolved/javaevolved.github.io.git
mkdir -p ~/.claude/skills
cp -R javaevolved.github.io/agent-plugins/modern-java-development/skills/modern-java \
  ~/.claude/skills/
```

For a project-only installation, copy the skill to
`.claude/skills/modern-java` in that project instead. See the
[Claude Code skills documentation](https://code.claude.com/docs/en/skills).

### OpenAI Codex CLI

Codex CLI also supports Agent Skills. Clone this repository and copy the skill
to your personal skills directory:

```bash
git clone --depth 1 https://github.com/javaevolved/javaevolved.github.io.git
mkdir -p ~/.agents/skills
cp -R javaevolved.github.io/agent-plugins/modern-java-development/skills/modern-java \
  ~/.agents/skills/
```

For a project-only installation, copy the skill to
`.agents/skills/modern-java` in that project instead. See the
[Codex skills documentation](https://developers.openai.com/codex/skills/).

Claude Code and Codex CLI use their own product-specific plugin manifests, so
the instructions above install the portable skill rather than the root
Agent Plugins manifest.

## Use

When the skill is active, the agent runs the detector from the Java project root:

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
  skills/modern-java/scripts/test_detect_java_version.py \
  skills/modern-java/scripts/test_reference_coverage.py
```

## Release

Run the **Release agent plugin** workflow from the `main` branch. The first run
publishes the manifest's initial `1.0.0` version. Later runs use a patch release
by default (`1.0.0` to `1.0.1`); select **Feature release** for a minor bump or
**Major release** for a major bump. The two release options are mutually
exclusive.
