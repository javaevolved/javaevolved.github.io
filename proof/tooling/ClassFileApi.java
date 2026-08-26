///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.nio.file.Files;
import java.nio.file.Path;

/// Proof: class-file-api
/// Source: content/tooling/class-file-api.yaml
void main() throws Exception {
    Path classFile = Files.createTempFile("ClassFileApi", ".class");
    try (var compiledClass =
            getClass().getResourceAsStream("ClassFileApi.class")) {
        Files.copy(compiledClass, classFile,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    try {
        ClassModel model = ClassFile.of().parse(classFile);
        model.methods().forEach(method ->
                System.out.println(
                        method.methodName().stringValue()));
    } finally {
        Files.delete(classFile);
    }
}
