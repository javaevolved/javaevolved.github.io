import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Dependency-free validation for the modern Java skill. */
public final class PluginValidationTest {
    private int assertions;

    public static void main(String[] args) throws Exception {
        PluginValidationTest tests = new PluginValidationTest();
        tests.runDetectorTests();
        tests.validateReferenceCoverage(
                args.length == 0 ? Paths.get("../..") : Paths.get(args[0]));
        System.out.println("Plugin validation passed (" + tests.assertions + " assertions)");
    }

    private void runDetectorTests() throws Exception {
        Path root = Files.createTempDirectory("java-version-detector-test");
        try {
            write(root.resolve("pom.xml"),
                    "<project><properties><java.version>17</java.version></properties>"
                            + "<build><plugins><plugin><configuration>"
                            + "<release>${java.version}</release>"
                            + "</configuration></plugin></plugins></build></project>");
            DetectJavaVersion.Result result = detect(root, null);
            assertEquals(17, result.selected.version, "Maven property release");
            assertEquals("maven-release", result.selected.source, "Maven release source");
            assertFalse(result.ambiguous, "single Maven release is unambiguous");

            Files.delete(root.resolve("pom.xml"));
            write(root.resolve("build.gradle.kts"),
                    "java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }\n"
                            + "tasks.withType<JavaCompile> { options.release.set(17) }\n");
            result = detect(root, null);
            assertEquals(17, result.selected.version, "Gradle release beats toolchain");
            assertEquals("gradle-release", result.selected.source, "Gradle release source");

            Files.delete(root.resolve("build.gradle.kts"));
            write(root.resolve("pom.xml"),
                    "<project><build><plugins><plugin><configuration>"
                            + "<jdkToolchain><version>21</version></jdkToolchain>"
                            + "</configuration></plugin></plugins></build></project>");
            result = detect(root, null);
            assertEquals(21, result.selected.version, "Maven toolchain version");
            assertEquals("maven-toolchain", result.selected.source, "Maven toolchain source");

            Files.delete(root.resolve("pom.xml"));
            write(root.resolve(".java-version"), "temurin-21.0.4\n");
            result = detect(root, null);
            assertEquals(21, result.selected.version, "version-manager file");
            assertEquals("java-version-file", result.selected.source, "version-manager source");

            write(root.resolve("pom.xml"),
                    "<project><properties><maven.compiler.release>17"
                            + "</maven.compiler.release></properties></project>");
            result = detect(root, "11");
            assertEquals(11, result.selected.version, "explicit version wins");
            assertEquals("explicit", result.selected.source, "explicit source");

            deleteTree(root);
            Files.createDirectories(root.resolve("api"));
            Files.createDirectories(root.resolve("app"));
            write(root.resolve("api/pom.xml"),
                    "<project><properties><maven.compiler.release>17"
                            + "</maven.compiler.release></properties></project>");
            write(root.resolve("app/pom.xml"),
                    "<project><properties><maven.compiler.release>21"
                            + "</maven.compiler.release></properties></project>");
            result = detect(root, null);
            assertTrue(result.ambiguous, "conflicting module releases are ambiguous");
            Set<Integer> versions = new HashSet<>();
            for (DetectJavaVersion.Candidate candidate : result.candidates) {
                if ("maven-release".equals(candidate.source)) {
                    versions.add(candidate.version);
                }
            }
            assertEquals(
                    new HashSet<Integer>(Arrays.asList(17, 21)),
                    versions,
                    "both module releases are reported");
            assertEquals(8, DetectJavaVersion.normalizeVersion("1.8.0_402"),
                    "legacy version normalization");
            assertEquals(8, DetectJavaVersion.normalizeVersion("jdk1.8.0_402"),
                    "prefixed legacy version normalization");
            assertEquals(null, DetectJavaVersion.normalizeVersion("1.4.2"),
                    "unsupported legacy version");
        } finally {
            deleteTree(root);
        }
    }

    private DetectJavaVersion.Result detect(Path root, String explicit) throws IOException {
        return DetectJavaVersion.detect(root, explicit, 3, null, "25");
    }

    private void validateReferenceCoverage(Path repository) throws IOException {
        Path content = repository.resolve("content");
        if (!Files.isDirectory(content)) {
            return;
        }
        Path references = repository.resolve(
                "agent-plugins/modern-java-development/skills/modern-java/references");
        Pattern marker = Pattern.compile("<!--\\s*covers:\\s*([^>]+)-->");
        Set<String> covered = new HashSet<>();
        try (Stream<Path> paths = Files.list(references)) {
            for (Path path : (Iterable<Path>) paths.filter(
                    candidate -> candidate.toString().endsWith(".md"))::iterator) {
                Matcher matcher = marker.matcher(read(path));
                while (matcher.find()) {
                    for (String slug : matcher.group(1).trim().split("\\s+")) {
                        covered.add(slug);
                    }
                }
            }
        }

        Set<String> missing = new HashSet<>();
        try (Stream<Path> paths = Files.walk(content, 2)) {
            for (Path path : (Iterable<Path>) paths.filter(
                    candidate -> candidate.toString().endsWith(".yaml")
                            && !"template.yaml".equals(candidate.getFileName().toString()))
                    ::iterator) {
                String filename = path.getFileName().toString();
                String slug = filename.substring(0, filename.length() - ".yaml".length());
                if (!covered.contains(slug)) {
                    missing.add(slug);
                }
            }
        }
        assertEquals(new HashSet<String>(), missing, "reference coverage");
    }

    private static void write(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.sorted((left, right) ->
                    right.getNameCount() - left.getNameCount())::iterator) {
                Files.delete(path);
            }
        }
    }

    private void assertTrue(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private void assertEquals(Object expected, Object actual, String message) {
        assertions++;
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(
                    message + ": expected " + expected + ", got " + actual);
        }
    }
}
