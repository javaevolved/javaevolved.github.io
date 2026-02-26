/// Proof: spring-api-versioning
/// Source: content/enterprise/spring-api-versioning.yaml
///
/// Note: Spring 7 API versioning feature — uses stub annotations to prove
/// the code structure compiles without Spring dependency.
@interface Configuration {}
@interface Override {}
@interface RestController {}
@interface RequestMapping { String value() default ""; }
@interface GetMapping { String value() default ""; String version() default ""; }
@interface PathVariable {}

record ProductDtoV1(Long id) {}
record ProductDtoV2(Long id, String name) {}

interface ApiVersionConfigurer {
    void useRequestHeader(String header);
}
interface WebMvcConfigurer {
    default void configureApiVersioning(ApiVersionConfigurer config) {}
}

// Configure versioning once
@Configuration
class WebConfig implements WebMvcConfigurer {
    @Override
    public void configureApiVersioning(
            ApiVersionConfigurer config) {
        config.useRequestHeader("X-API-Version");
    }
}

interface ProductService {
    ProductDtoV1 getV1(Long id);
    ProductDtoV2 getV2(Long id);
}

// Single controller, version per method
@RestController
@RequestMapping("/api/products")
class ProductController {
    ProductService service;

    @GetMapping(value = "/{id}", version = "1")
    public ProductDtoV1 getV1(@PathVariable Long id) {
        return service.getV1(id);
    }

    @GetMapping(value = "/{id}", version = "2")
    public ProductDtoV2 getV2(@PathVariable Long id) {
        return service.getV2(id);
    }
}

void main() {}
