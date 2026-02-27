///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0
//DEPS jakarta.annotation:jakarta.annotation-api:3.0.0

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.PostConstruct;
import java.util.*;

/// Proof: singleton-ejb-vs-cdi-application-scoped
/// Source: content/enterprise/singleton-ejb-vs-cdi-application-scoped.yaml
@ApplicationScoped
class ConfigCache {
    private volatile Map<String, String> cache;

    @PostConstruct
    public void load() {
        cache = loadFromDatabase();
    }

    public String get(String key) {
        return cache.get(key);
    }

    public void refresh() {
        cache = loadFromDatabase();
    }

    private Map<String, String> loadFromDatabase() {
        return Map.of("key", "value");
    }
}

void main() {}
