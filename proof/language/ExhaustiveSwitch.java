/// Proof: exhaustive-switch
/// Source: content/language/exhaustive-switch.yaml
sealed interface Shape permits Circle, Rect {}
record Circle(double r) implements Shape {}
record Rect(double w, double h) implements Shape {}

// sealed Shape permits Circle, Rect
double area(Shape s) {
    return switch (s) {
        case Circle c ->
            Math.PI * c.r() * c.r();
        case Rect r ->
            r.w() * r.h();
    }; // no default needed!
}

void main() {
    area(new Circle(1));
    area(new Rect(2, 3));
}
