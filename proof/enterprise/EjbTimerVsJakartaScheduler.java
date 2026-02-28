///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25+
//DEPS jakarta.enterprise:jakarta.enterprise.cdi-api:4.1.0
//DEPS jakarta.annotation:jakarta.annotation-api:3.0.0
//DEPS jakarta.enterprise.concurrent:jakarta.enterprise.concurrent-api:3.1.0

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/// Proof: ejb-timer-vs-jakarta-scheduler
/// Source: content/enterprise/ejb-timer-vs-jakarta-scheduler.yaml
@ApplicationScoped
class ReportGenerator {
    @Resource
    ManagedScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        scheduler.scheduleAtFixedRate(
            this::generateReport,
            0, 24, TimeUnit.HOURS);
    }

    public void generateReport() {
        buildDailyReport();
    }

    void buildDailyReport() {}
}

void main() {}
