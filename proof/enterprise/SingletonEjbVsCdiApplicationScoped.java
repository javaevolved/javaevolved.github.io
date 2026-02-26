import java.util.*;
import java.util.function.*;

/// Proof: singleton-ejb-vs-cdi-application-scoped
/// Source: content/enterprise/singleton-ejb-vs-cdi-application-scoped.yaml
@interface ApplicationScoped {}
@interface PostConstruct {}

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
