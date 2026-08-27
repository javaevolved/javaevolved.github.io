///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+

/// Proof: class-newinstance-to-constructor
/// Source: content/tooling/class-newinstance-to-constructor.yaml
interface Plugin {}

static final class ExamplePlugin implements Plugin {}

void main() throws ReflectiveOperationException {
    Class<? extends Plugin> pluginClass = ExamplePlugin.class;

    Plugin plugin = pluginClass
            .getDeclaredConstructor()
            .newInstance();

    IO.println(plugin.getClass().getSimpleName());
}
