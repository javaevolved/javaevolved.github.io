/// Proof: servlet-vs-jaxrs
/// Source: content/enterprise/servlet-vs-jaxrs.yaml
@interface Path { String value() default ""; }
@interface GET {}
@interface Produces { String[] value(); }
@interface QueryParam { String value(); }

record User(String id) {}

record Response(Object entity) {
    static Response ok(Object entity) { return new Response(entity); }
    Response build() { return this; }
}

class MediaType {
    static final String APPLICATION_JSON = "application/json";
}

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
