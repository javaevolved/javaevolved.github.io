/// Proof: markdown-javadoc-comments
/// Source: content/language/markdown-javadoc-comments.yaml
record User(int id, String name) {}

/// Returns the `User` with
/// the given ID.
///
/// Example:
/// ```java
/// var user = findUser(123);
/// ```
///
/// @param id the user ID
/// @return the user
public User findUser(int id) {
    return new User(id, "Alice");
}

void main() {
    findUser(1);
}
