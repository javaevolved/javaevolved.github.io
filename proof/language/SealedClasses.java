/// Proof: sealed-classes
/// Source: content/language/sealed-classes.yaml
public sealed interface Shape
    permits Circle, Rect {}
public record Circle(double r)
    implements Shape {}
public record Rect(double w, double h)
    implements Shape {}

void main() {
    Shape s = new Circle(5);
}
