///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS org.springframework:spring-webmvc:7.0.5

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/// Proof: spring-boot-mvc-config
/// Source: content/enterprise/spring-boot-mvc-config.yaml
@Configuration
class WebConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("home");
    }
}

void main() {}
