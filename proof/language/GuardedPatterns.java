///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: guarded-patterns
/// Source: content/language/guarded-patterns.yaml
sealed interface Shape permits Circle, Other {}
record Circle(double radius) implements Shape {}
record Other() implements Shape {}

String describe(Shape shape) {
    return switch (shape) {
        case Circle c
            when c.radius() > 10
                -> "large circle";
        case Circle c
                -> "small circle";
        default -> "not a circle";
    };
}

void main() {
    describe(new Circle(5));
    describe(new Circle(15));
    describe(new Other());
}
