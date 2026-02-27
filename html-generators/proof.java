///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.3
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.18.3

import module java.base;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Runs all proofCode snippets found in content/**\/\*.yaml through JShell to verify
 * they compile and execute without errors.
 *
 * Usage:
 *   jbang html-generators/proof.java           # run all proofCode snippets
 *   jbang html-generators/proof.java --list    # list patterns with/without proofCode
 */
static final String CONTENT_DIR = "content";
static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

/** Determine the JShell executable path from the running JDK */
static String jshellPath() {
    var javaHome = System.getProperty("java.home");
    var jshell = Path.of(javaHome, "bin", "jshell");
    if (Files.exists(jshell)) return jshell.toString();
    return "jshell"; // fall back to PATH
}

void main(String... args) throws Exception {
    boolean listMode = args.length > 0 && args[0].equals("--list");

    var contentDir = Path.of(CONTENT_DIR);
    if (!Files.isDirectory(contentDir)) {
        IO.println("ERROR: content directory not found. Run from the repository root.");
        System.exit(1);
    }

    var yamlFiles = Files.walk(contentDir)
            .filter(p -> {
                var name = p.getFileName().toString();
                return name.endsWith(".yaml") || name.endsWith(".yml");
            })
            .sorted()
            .toList();

    if (listMode) {
        listPatterns(yamlFiles);
        return;
    }

    int passed = 0, failed = 0, skipped = 0;
    var failures = new ArrayList<String>();

    for (var file : yamlFiles) {
        JsonNode node;
        try {
            node = YAML_MAPPER.readTree(file.toFile());
        } catch (Exception e) {
            IO.println("WARN: Could not parse " + file + ": " + e.getMessage());
            continue;
        }

        var proofNode = node.get("proofCode");
        if (proofNode == null || proofNode.isNull() || proofNode.asText().isBlank()) {
            skipped++;
            continue;
        }

        var slug = node.has("slug") ? node.get("slug").asText() : file.getFileName().toString();
        var category = node.has("category") ? node.get("category").asText() : "unknown";
        var proofCode = proofNode.asText();

        System.out.print("  [" + category + "/" + slug + "] ");

        try {
            // Pipe proofCode + /exit via stdin so JShell exits automatically
            var proc = new ProcessBuilder(jshellPath(), "--feedback", "concise", "-")
                    .redirectErrorStream(true)
                    .start();

            try (var stdin = proc.getOutputStream()) {
                stdin.write((proofCode + "\n/exit\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            var output = proc.inputReader().lines().collect(java.util.stream.Collectors.joining("\n"));
            proc.waitFor(30, TimeUnit.SECONDS);

            if (isFailure(output)) {
                IO.println("FAILED");
                // Print the output indented for readability
                for (var line : output.split("\n")) {
                    if (!line.isBlank()) IO.println("    " + line);
                }
                failed++;
                failures.add(category + "/" + slug);
            } else {
                IO.println("OK");
                passed++;
            }
        } catch (Exception e) {
            IO.println("ERROR: " + e.getMessage());
            failed++;
            failures.add(category + "/" + slug);
        }
    }

    IO.println("");
    IO.println("Results: %d passed, %d failed, %d skipped (no proofCode)".formatted(passed, failed, skipped));

    if (failed > 0) {
        IO.println("");
        IO.println("Failed patterns:");
        for (var f : failures) IO.println("  - " + f);
        System.exit(1);
    }
}

/** Returns true if the JShell output indicates a compile or runtime error */
static boolean isFailure(String output) {
    for (var line : output.split("\n")) {
        var stripped = line.strip();
        if (stripped.startsWith("Error:") || stripped.startsWith("|  Error")) return true;
        if (stripped.startsWith("Exception ") && stripped.contains(".")) return true;
    }
    return false;
}

/** Lists all patterns and whether they have proofCode */
static void listPatterns(List<Path> yamlFiles) throws Exception {
    int withProof = 0, withoutProof = 0;
    IO.println("Patterns with proofCode:");
    for (var file : yamlFiles) {
        JsonNode node;
        try {
            node = YAML_MAPPER.readTree(file.toFile());
        } catch (Exception e) {
            continue;
        }
        var slug = node.has("slug") ? node.get("slug").asText() : file.toString();
        var category = node.has("category") ? node.get("category").asText() : "?";
        var hasProof = node.has("proofCode") && !node.get("proofCode").isNull()
                && !node.get("proofCode").asText().isBlank();
        if (hasProof) {
            IO.println("  [x] " + category + "/" + slug);
            withProof++;
        } else {
            withoutProof++;
        }
    }
    IO.println("");
    IO.println("Patterns without proofCode (%d):".formatted(withoutProof));
    for (var file : yamlFiles) {
        JsonNode node;
        try {
            node = YAML_MAPPER.readTree(file.toFile());
        } catch (Exception e) {
            continue;
        }
        var slug = node.has("slug") ? node.get("slug").asText() : file.toString();
        var category = node.has("category") ? node.get("category").asText() : "?";
        var hasProof = node.has("proofCode") && !node.get("proofCode").isNull()
                && !node.get("proofCode").asText().isBlank();
        if (!hasProof) IO.println("  [ ] " + category + "/" + slug);
    }
    IO.println("");
    IO.println("Total: %d with proofCode, %d without".formatted(withProof, withoutProof));
}
