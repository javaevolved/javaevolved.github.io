import java.util.random.*;

/// Proof: random-generator
/// Source: content/security/random-generator.yaml
void main() {
    // Algorithm-agnostic via factory
    var rng = RandomGenerator.of("L64X128MixRandom");
    int value = rng.nextInt(100);

    // Or get a splittable generator
    var rng2 = RandomGeneratorFactory
        .<RandomGenerator.SplittableGenerator>of("L64X128MixRandom").create();
}
