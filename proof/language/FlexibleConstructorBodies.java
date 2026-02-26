/// Proof: flexible-constructor-bodies
/// Source: content/language/flexible-constructor-bodies.yaml
class Shape {
    final double width, height;
    Shape(double width, double height) {
        this.width = width;
        this.height = height;
    }
}

class Square extends Shape {
    Square(double side) {
        if (side <= 0)
            throw new IllegalArgumentException("bad");
        super(side, side);
    }
}

void main() {
    new Square(5);
}
