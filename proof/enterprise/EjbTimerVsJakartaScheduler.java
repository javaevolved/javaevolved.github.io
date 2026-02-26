import java.util.concurrent.*;

/// Proof: ejb-timer-vs-jakarta-scheduler
/// Source: content/enterprise/ejb-timer-vs-jakarta-scheduler.yaml
@interface ApplicationScoped {}
@interface Resource {}
@interface PostConstruct {}

interface ManagedScheduledExecutorService
        extends ScheduledExecutorService {}

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
