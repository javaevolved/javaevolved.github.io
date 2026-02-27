///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.ws.rs:jakarta.ws.rs-api:4.0.0

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;

/// Proof: servlet-vs-jaxrs
/// Source: content/enterprise/servlet-vs-jaxrs.yaml
record User(String id) {}

@Path("/users")
class UserResource {
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(
            @QueryParam("id") String id) {
        return Response.ok(new User(id)).build();
    }
}

void main() {}
