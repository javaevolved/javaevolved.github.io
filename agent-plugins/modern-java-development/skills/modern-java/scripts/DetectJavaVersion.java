import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Detects a Java project's effective compilation target. */
public final class DetectJavaVersion {
    private static final Set<String> IGNORED_DIRS = new HashSet<>(Arrays.asList(
            ".git", ".gradle", ".idea", ".mvn", ".vscode", "build",
            "node_modules", "out", "target"));

    private static final Map<String, Integer> SOURCE_PRIORITY = new HashMap<>();

    static {
        SOURCE_PRIORITY.put("explicit", 100);
        SOURCE_PRIORITY.put("maven-release", 90);
        SOURCE_PRIORITY.put("gradle-release", 90);
        SOURCE_PRIORITY.put("maven-toolchain", 80);
        SOURCE_PRIORITY.put("gradle-toolchain", 80);
        SOURCE_PRIORITY.put("maven-source", 70);
        SOURCE_PRIORITY.put("gradle-source", 70);
        SOURCE_PRIORITY.put("java-version-file", 60);
        SOURCE_PRIORITY.put("sdkman", 60);
        SOURCE_PRIORITY.put("asdf", 60);
        SOURCE_PRIORITY.put("ci", 50);
        SOURCE_PRIORITY.put("environment", 40);
        SOURCE_PRIORITY.put("runtime", 10);
    }

    private DetectJavaVersion() {
    }

    static final class Candidate {
        final int version;
        final String source;
        final String location;
        final String raw;
        final int priority;

        Candidate(int version, String source, String location, String raw) {
            this.version = version;
            this.source = source;
            this.location = location;
            this.raw = raw;
            this.priority = SOURCE_PRIORITY.get(source);
        }

        String key() {
            return version + "\0" + source + "\0" + location + "\0" + raw;
        }
    }

    static final class Result {
        final Path root;
        final Candidate selected;
        final boolean ambiguous;
        final List<Candidate> conflicts;
        final List<Candidate> candidates;

        Result(
                Path root,
                Candidate selected,
                boolean ambiguous,
                List<Candidate> conflicts,
                List<Candidate> candidates) {
            this.root = root;
            this.selected = selected;
            this.ambiguous = ambiguous;
            this.conflicts = conflicts;
            this.candidates = candidates;
        }
    }

    static Integer normalizeVersion(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim().replaceAll("^[\"']|[\"']$", "");
        Matcher match = Pattern.compile(
                "(?<!\\d)(?:1\\.)?(\\d{1,3})(?:[._+\\-]\\d+)*").matcher(text);
        if (!match.find()) {
            return null;
        }
        int version = Integer.parseInt(match.group(1));
        return version >= 5 ? version : null;
    }

    private static void addCandidate(
            List<Candidate> candidates, String value, String source, Object location) {
        Integer version = normalizeVersion(value);
        if (version != null) {
            candidates.add(new Candidate(
                    version, source, String.valueOf(location), value.trim()));
        }
    }

    private static String localName(Node node) {
        String name = node.getLocalName();
        return name != null ? name : node.getNodeName().replaceFirst("^.*:", "");
    }

    private static String resolveMavenValue(String value, Map<String, String> properties) {
        Set<String> seen = new HashSet<>();
        String current = value.trim();
        while (true) {
            Matcher match = Pattern.compile("^\\$\\{([^}]+)}$").matcher(current);
            if (!match.matches() || !seen.add(match.group(1))) {
                return current;
            }
            current = properties.getOrDefault(match.group(1), current).trim();
        }
    }

    private static void inspectMaven(Path path, List<Candidate> candidates) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Element root = factory.newDocumentBuilder().parse(path.toFile()).getDocumentElement();

            Map<String, String> properties = new HashMap<>();
            NodeList all = root.getElementsByTagName("*");
            for (int index = 0; index < all.getLength(); index++) {
                Node node = all.item(index);
                if ("properties".equals(localName(node))) {
                    NodeList children = node.getChildNodes();
                    for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                        Node child = children.item(childIndex);
                        if (child.getNodeType() == Node.ELEMENT_NODE) {
                            properties.put(localName(child), child.getTextContent().trim());
                        }
                    }
                }
            }

            for (int index = 0; index < all.getLength(); index++) {
                Node node = all.item(index);
                String name = localName(node);
                String value = resolveMavenValue(node.getTextContent(), properties);
                if ("jdkToolchain".equals(name)) {
                    NodeList children = ((Element) node).getElementsByTagName("*");
                    for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                        Node child = children.item(childIndex);
                        if ("version".equals(localName(child))) {
                            addCandidate(
                                    candidates,
                                    resolveMavenValue(child.getTextContent(), properties),
                                    "maven-toolchain",
                                    path);
                        }
                    }
                } else if ("maven.compiler.release".equals(name) || "release".equals(name)) {
                    addCandidate(candidates, value, "maven-release", path);
                } else if ("maven.compiler.source".equals(name) || "source".equals(name)) {
                    addCandidate(candidates, value, "maven-source", path);
                }
            }

            for (String key : Arrays.asList("java.version", "jdk.version")) {
                if (properties.containsKey(key)) {
                    addCandidate(candidates, properties.get(key), "maven-source", path);
                }
            }
        } catch (Exception ignored) {
            // Malformed or unreadable build metadata is not evidence of a Java target.
        }
    }

    private static void inspectGradle(Path path, List<Candidate> candidates) {
        String text;
        try {
            text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return;
        }
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        patterns.put("gradle-release", Pattern.compile(
                "(?:options\\.)?release(?:\\.set)?\\s*\\(?\\s*(\\d{1,3})"));
        patterns.put("gradle-toolchain", Pattern.compile(
                "JavaLanguageVersion\\.of\\s*\\(\\s*(\\d{1,3})\\s*\\)"));
        patterns.put("gradle-source", Pattern.compile(
                "(?:sourceCompatibility|targetCompatibility)\\s*=\\s*"
                        + "(?:JavaVersion\\.VERSION_)?[\"']?(?:1[_.])?(\\d{1,3})"));
        for (Map.Entry<String, Pattern> entry : patterns.entrySet()) {
            Matcher matcher = entry.getValue().matcher(text);
            while (matcher.find()) {
                addCandidate(candidates, matcher.group(1), entry.getKey(), path);
            }
        }
    }

    private static void inspectVersionFiles(Path root, List<Candidate> candidates) {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(".java-version", "java-version-file");
        files.put(".sdkmanrc", "sdkman");
        files.put(".tool-versions", "asdf");
        for (Map.Entry<String, String> entry : files.entrySet()) {
            Path path = root.resolve(entry.getKey());
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                Matcher matcher;
                String value = null;
                if (".sdkmanrc".equals(entry.getKey())) {
                    matcher = Pattern.compile("(?m)^\\s*java\\s*=\\s*(\\S+)").matcher(text);
                    value = matcher.find() ? matcher.group(1) : null;
                } else if (".tool-versions".equals(entry.getKey())) {
                    matcher = Pattern.compile("(?m)^\\s*java\\s+(\\S+)").matcher(text);
                    value = matcher.find() ? matcher.group(1) : null;
                } else if (!text.isEmpty()) {
                    value = text.split("\\R", 2)[0];
                }
                addCandidate(candidates, value, entry.getValue(), path);
            } catch (IOException ignored) {
                // Unreadable version-manager files are not evidence of a target.
            }
        }
    }

    private static void inspectCi(Path root, List<Candidate> candidates) {
        Path workflows = root.resolve(".github").resolve("workflows");
        if (!Files.isDirectory(workflows)) {
            return;
        }
        try (java.util.stream.Stream<Path> paths = Files.list(workflows)) {
            paths.filter(path -> {
                        String name = path.getFileName().toString();
                        return name.endsWith(".yml") || name.endsWith(".yaml");
                    })
                    .sorted()
                    .forEach(path -> {
                        try {
                            String text = new String(
                                    Files.readAllBytes(path), StandardCharsets.UTF_8);
                            Matcher matcher = Pattern.compile(
                                    "(?m)^\\s*java-version\\s*:\\s*[\"']?([^\"'\\s#]+)")
                                    .matcher(text);
                            while (matcher.find()) {
                                addCandidate(candidates, matcher.group(1), "ci", path);
                            }
                        } catch (IOException ignored) {
                            // Continue inspecting other workflows.
                        }
                    });
        } catch (IOException ignored) {
            // An unreadable workflow directory is not evidence of a target.
        }
    }

    private static List<Path> buildFiles(Path root, int maxDepth) throws IOException {
        List<Path> result = new ArrayList<>();
        Files.walkFileTree(root, Collections.emptySet(), maxDepth + 1,
                new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(
                            Path directory, BasicFileAttributes attributes) {
                        if (!directory.equals(root)
                                && IGNORED_DIRS.contains(directory.getFileName().toString())) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(
                            Path file, BasicFileAttributes attributes) {
                        String name = file.getFileName().toString();
                        if ("pom.xml".equals(name)
                                || "build.gradle".equals(name)
                                || "build.gradle.kts".equals(name)) {
                            result.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
        Collections.sort(result);
        return result;
    }

    static Result detect(
            Path root,
            String explicit,
            int maxDepth,
            String environmentVersion,
            String runtimeVersion) throws IOException {
        List<Candidate> candidates = new ArrayList<>();
        addCandidate(candidates, explicit, "explicit", "command line");
        for (Path path : buildFiles(root, maxDepth)) {
            if ("pom.xml".equals(path.getFileName().toString())) {
                inspectMaven(path, candidates);
            } else {
                inspectGradle(path, candidates);
            }
        }
        inspectVersionFiles(root, candidates);
        inspectCi(root, candidates);
        addCandidate(candidates, environmentVersion, "environment", "JAVA_VERSION");
        addCandidate(candidates, runtimeVersion, "runtime", "java on PATH");

        Map<String, Candidate> unique = new LinkedHashMap<>();
        for (Candidate candidate : candidates) {
            unique.put(candidate.key(), candidate);
        }
        List<Candidate> ordered = new ArrayList<>(unique.values());
        ordered.sort(Comparator
                .comparingInt((Candidate candidate) -> candidate.priority).reversed()
                .thenComparing(candidate -> candidate.location)
                .thenComparingInt(candidate -> candidate.version));

        Candidate selected = ordered.isEmpty() ? null : ordered.get(0);
        Set<Integer> strongestVersions = new HashSet<>();
        List<Candidate> conflicts = new ArrayList<>();
        if (selected != null) {
            for (Candidate candidate : ordered) {
                if (candidate.priority == selected.priority) {
                    strongestVersions.add(candidate.version);
                }
                if (candidate.version != selected.version && candidate.priority >= 70) {
                    conflicts.add(candidate);
                }
            }
        }
        return new Result(
                root,
                selected,
                strongestVersions.size() > 1,
                conflicts,
                ordered);
    }

    private static String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.toString();
    }

    private static String candidateJson(Candidate candidate, String indent) {
        if (candidate == null) {
            return "null";
        }
        return "{\n"
                + indent + "  \"version\": " + candidate.version + ",\n"
                + indent + "  \"source\": \"" + escapeJson(candidate.source) + "\",\n"
                + indent + "  \"location\": \"" + escapeJson(candidate.location) + "\",\n"
                + indent + "  \"raw\": \"" + escapeJson(candidate.raw) + "\",\n"
                + indent + "  \"priority\": " + candidate.priority + "\n"
                + indent + "}";
    }

    private static String candidatesJson(List<Candidate> candidates, String indent) {
        if (candidates.isEmpty()) {
            return "[]";
        }
        StringBuilder json = new StringBuilder("[\n");
        for (int index = 0; index < candidates.size(); index++) {
            if (index > 0) {
                json.append(",\n");
            }
            json.append(indent).append("  ")
                    .append(candidateJson(candidates.get(index), indent + "  "));
        }
        return json.append("\n").append(indent).append("]").toString();
    }

    private static String resultJson(Result result) {
        return "{\n"
                + "  \"root\": \"" + escapeJson(result.root.toString()) + "\",\n"
                + "  \"selected\": " + candidateJson(result.selected, "  ") + ",\n"
                + "  \"ambiguous\": " + result.ambiguous + ",\n"
                + "  \"conflicts\": " + candidatesJson(result.conflicts, "  ") + ",\n"
                + "  \"candidates\": " + candidatesJson(result.candidates, "  ") + "\n"
                + "}";
    }

    private static void usage(PrintStream stream) {
        stream.println("Usage: detect-java-version [path]"
                + " [--java-version VERSION] [--max-depth DEPTH]");
    }

    public static void main(String[] args) {
        Path path = Paths.get(".");
        String explicit = null;
        int maxDepth = 3;
        boolean pathSet = false;
        boolean optionsEnded = false;
        try {
            for (int index = 0; index < args.length; index++) {
                if (!optionsEnded && "--".equals(args[index])) {
                    optionsEnded = true;
                    continue;
                }
                if (!optionsEnded && args[index].startsWith("--java-version=")) {
                    explicit = args[index].substring("--java-version=".length());
                    continue;
                }
                if (!optionsEnded && args[index].startsWith("--max-depth=")) {
                    maxDepth = Math.max(0, Integer.parseInt(
                            args[index].substring("--max-depth=".length())));
                    continue;
                }
                if (optionsEnded) {
                    if (pathSet) {
                        throw new IllegalArgumentException(
                                "unexpected argument: " + args[index]);
                    }
                    path = Paths.get(args[index]);
                    pathSet = true;
                    continue;
                }
                switch (args[index]) {
                    case "--java-version":
                        explicit = args[++index];
                        break;
                    case "--max-depth":
                        maxDepth = Math.max(0, Integer.parseInt(args[++index]));
                        break;
                    case "--help":
                    case "-h":
                        usage(System.out);
                        return;
                    default:
                        if (args[index].startsWith("-") || pathSet) {
                            throw new IllegalArgumentException("unexpected argument: " + args[index]);
                        }
                        path = Paths.get(args[index]);
                        pathSet = true;
                }
            }

            Path root = path.toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                System.err.println("{\"error\":\"not a directory: "
                        + escapeJson(root.toString()) + "\"}");
                System.exit(2);
            }
            if (explicit != null && normalizeVersion(explicit) == null) {
                System.err.println("{\"error\":\"invalid Java version: "
                        + escapeJson(explicit) + "\"}");
                System.exit(2);
            }
            Result result = detect(
                    root,
                    explicit,
                    maxDepth,
                    System.getenv("JAVA_VERSION"),
                    System.getProperty("java.specification.version"));
            System.out.println(resultJson(result));
            if (result.selected == null) {
                System.exit(1);
            }
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException exception) {
            usage(System.err);
            System.exit(2);
        } catch (IllegalArgumentException | IOException exception) {
            System.err.println("{\"error\":\"" + escapeJson(exception.getMessage()) + "\"}");
            System.exit(2);
        }
    }
}
