/// Proof: soap-vs-jakarta-rest
/// Source: content/enterprise/soap-vs-jakarta-rest.yaml
@interface Path { String value() default ""; }
@interface Produces { String[] value(); }
@interface GET {}
@interface PathParam { String value(); }
@interface Inject {}

record User(String id, String name) {}

class MediaType {
    static final String APPLICATION_JSON = "application/json";
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
