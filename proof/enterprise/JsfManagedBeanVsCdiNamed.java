///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.*;

/// Proof: jsf-managed-bean-vs-cdi-named
/// Source: content/enterprise/jsf-managed-bean-vs-cdi-named.yaml
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
