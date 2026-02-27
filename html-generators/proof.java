///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25

import module java.base;

/**
 * Runs all proof scripts found in proof/**\/\*.java through JBang to verify
 * they compile and execute without errors.
 *
 * Each proof/<category>/SlugName.java is a standalone JBang script that
 * demonstrates a modern Java pattern from the corresponding content YAML file.
 *
 * Usage:
 *   jbang html-generators/proof.java           # run all proof scripts
 *   jbang html-generators/proof.java --list    # list all proof scripts
 */
static final String PROOF_DIR = "proof";

/** Find the jbang executable. Prefers JBANG env var, then PATH. */
static String jbangPath() {
    var envJbang = System.getenv("JBANG_HOME");
    if (envJbang != null) {
        var p = Path.of(envJbang, "bin", "jbang");
        if (Files.exists(p)) return p.toString();
    }
    // When proof.java is run via jbang, jbang itself is in PATH
    return "jbang";
}

void main(String... args) throws Exception {
    boolean listMode = args.length > 0 && args[0].equals("--list");

    var proofDir = Path.of(PROOF_DIR);
    if (!Files.isDirectory(proofDir)) {
        IO.println("ERROR: proof directory not found. Run from the repository root.");
        System.exit(1);
    }

    var proofFiles = Files.walk(proofDir)
            .filter(p -> p.toString().endsWith(".java"))
            .sorted()
            .toList();

    if (proofFiles.isEmpty()) {
        IO.println("No proof scripts found in " + PROOF_DIR + "/");
        System.exit(1);
    }

    if (listMode) {
        IO.println("Proof scripts (%d):".formatted(proofFiles.size()));
        for (var file : proofFiles) {
            var label = proofLabel(file);
            IO.println("  " + label);
        }
        return;
    }

    IO.println("Running %d proof scripts...".formatted(proofFiles.size()));
    IO.println("");

    int passed = 0, failed = 0;
    var failures = new ArrayList<String>();

    for (var file : proofFiles) {
        var label = proofLabel(file);
        System.out.print("  [" + label + "] ");

        try {
            var proc = new ProcessBuilder(jbangPath(), "--quiet", file.toString())
                    .redirectErrorStream(true)
                    .start();

            var output = proc.inputReader().lines().collect(java.util.stream.Collectors.joining("\n"));
            boolean timedOut = !proc.waitFor(60, TimeUnit.SECONDS);

            if (timedOut) {
                proc.destroyForcibly();
                IO.println("TIMEOUT");
                failed++;
                failures.add(label);
            } else if (proc.exitValue() != 0) {
                IO.println("FAILED");
                for (var line : output.split("\n")) {
                    if (!line.isBlank()) IO.println("    " + line);
                }
                failed++;
                failures.add(label);
            } else {
                IO.println("OK");
                passed++;
            }
        } catch (Exception e) {
            IO.println("ERROR: " + e.getMessage());
            failed++;
            failures.add(label);
        }
    }

    IO.println("");
    IO.println("Results: %d passed, %d failed".formatted(passed, failed));

    if (failed > 0) {
        IO.println("");
        IO.println("Failed:");
        for (var f : failures) IO.println("  - " + f);
        System.exit(1);
    }
}

/** Derive a human-readable label from the proof file path, e.g. "language/TypeInferenceWithVar" */
static String proofLabel(Path file) {
    var parts = new ArrayList<String>();
    var iter = Path.of(PROOF_DIR).relativize(file).iterator();
    while (iter.hasNext()) parts.add(iter.next().toString());
    var name = parts.getLast().replaceAll("\\.java$", "");
    var category = parts.size() > 1 ? parts.getFirst() : "?";
    return category + "/" + name;
}
