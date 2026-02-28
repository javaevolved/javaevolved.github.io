///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.ws.rs:jakarta.ws.rs-api:4.0.0
//DEPS jakarta.inject:jakarta.inject-api:2.0.1

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.inject.Inject;

/// Proof: soap-vs-jakarta-rest
/// Source: content/enterprise/soap-vs-jakarta-rest.yaml
class User {
    String id; String name;
    User(String id, String name) { this.id = id; this.name = name; }
}

interface UserService {
    User findById(String id);
}

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
class UserResource {
    @Inject
    UserService userService;

    @GET
    @Path("/{id}")
    public User getUser(@PathParam("id") String id) {
        return userService.findById(id);
    }
}

void main() {}
