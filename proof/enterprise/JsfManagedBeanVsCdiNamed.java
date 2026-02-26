import java.io.*;

/// Proof: jsf-managed-bean-vs-cdi-named
/// Source: content/enterprise/jsf-managed-bean-vs-cdi-named.yaml
@interface Named {}
@interface SessionScoped {}
@interface Inject {}

interface UserService {
    String findName(String id);
}

@Named
@SessionScoped
class UserBean implements Serializable {
    @Inject
    private UserService userService;

    private String name;

    public String getName() { return name; }
    public void setName(String name) {
        this.name = name;
    }
}

void main() {}
