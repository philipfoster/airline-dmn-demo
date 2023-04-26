


import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.kie.dmn.api.core.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusMeterRegistry;

@ApplicationScoped
public class DmnMetricsCollector implements DMNRuntimeEventListener {

    private static final Logger logger = LoggerFactory.getLogger(DmnMetricsCollector.class);
    private static final String TIMER_KEY = "__timerKey";

    private final PrometheusMeterRegistry prometheusMeterRegistry;

    private final Map<String, Instant> timers = new ConcurrentHashMap<>();

    private static final String RULE_DURATION_METRIC_KEY = "rule.execution.duration";
    private final Timer timerMetrics;

    @Inject
    public DmnMetricsCollector(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.prometheusMeterRegistry = prometheusMeterRegistry;

        timerMetrics = Timer.builder(RULE_DURATION_METRIC_KEY)
                .publishPercentiles(0.1, 0.25, 0.5, 0.75, 0.9, 0.99)
                .publishPercentileHistogram()
                .register(prometheusMeterRegistry);
    }

    @Override
    public void beforeEvaluateAll(BeforeEvaluateAllEvent event) {

        // Inject a tracking key into the rule context, so we can match rules to their start times in afterEvaluateAll()
        String trackingKey = UUID.randomUUID().toString();
        Instant start = Instant.now();
        timers.put(trackingKey, start);
        event.getResult().getContext().set(TIMER_KEY, trackingKey);
        DMNRuntimeEventListener.super.beforeEvaluateAll(event);
    }

    @Override
    public void afterEvaluateAll(AfterEvaluateAllEvent event) {
        DMNRuntimeEventListener.super.afterEvaluateAll(event);

        Instant stop = Instant.now();
        String trackingKey = event.getResult().getContext().get(TIMER_KEY).toString();
        Instant start = timers.remove(trackingKey);
        Duration duration = Duration.between(start, stop);
        logger.info("Duration for {} = {}", trackingKey, duration);

        timerMetrics.record(duration);
    }
}

