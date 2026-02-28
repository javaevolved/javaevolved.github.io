///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
/// Proof: static-members-in-inner-classes
/// Source: content/language/static-members-in-inner-classes.yaml
class Library {
    // Can be inner class with statics
    class Book {
        static int globalBookCount;

        Book() {
            Book.globalBookCount++;
        }
    }
}

void main() {
    // Usage
    var lib = new Library();
    var book = lib.new Book();
}
