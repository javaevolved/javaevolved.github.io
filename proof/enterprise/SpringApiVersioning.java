///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS org.springframework:spring-webmvc:7.0.5

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.*;

/// Proof: spring-api-versioning
/// Source: content/enterprise/spring-api-versioning.yaml
record ProductDtoV1(Long id) {}
record ProductDtoV2(Long id, String name) {}

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
