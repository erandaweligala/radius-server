package com.csg.airtel.aaa4j.domain.producer;

import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicLong;

import static io.quarkus.arc.ComponentsProvider.LOG;

@ApplicationScoped
public class RadiusAccountingProducer {

    private static final int TPS_WINDOW_SIZE_MS = 1000;
    private static final int DEFAULT_MAX_TPS = 1000;

    private final Emitter<AccountingRequestDto> accountingEmitter;
    private final Counter failureCounter;
    private final Counter fallbackCounter;
    private final Counter tpsExceededCounter;
    private final Counter totalRequestsCounter;
    private final AtomicLong consecutiveFailures = new AtomicLong(0);
    private final AtomicLong currentWindowRequests = new AtomicLong(0);
    private final AtomicLong windowStartTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong currentTps = new AtomicLong(0);

    private final int maxTps;

    @Inject
    public RadiusAccountingProducer(
            @Channel("accounting-events") Emitter<AccountingRequestDto> accountingEmitter,
            MeterRegistry meterRegistry,
            @ConfigProperty(name = "accounting.max.tps", defaultValue = "1000") int maxTps) {
        this.accountingEmitter = accountingEmitter;
        this.maxTps = maxTps;
        this.failureCounter = meterRegistry.counter("accounting.publish.failures");
        this.fallbackCounter = meterRegistry.counter("accounting.publish.fallback");
        this.tpsExceededCounter = meterRegistry.counter("accounting.tps.exceeded");
        this.totalRequestsCounter = meterRegistry.counter("accounting.requests.total");

        Gauge.builder("accounting.tps.current", currentTps, AtomicLong::get)
                .description("Current transactions per second")
                .register(meterRegistry);

        Gauge.builder("accounting.tps.limit", () -> maxTps)
                .description("Maximum allowed transactions per second")
                .register(meterRegistry);

        LOG.infof("RadiusAccountingProducer initialized with max TPS limit: %d", maxTps);
    }

    @Timeout(3000) // 3 second timeout for Kafka publish operations
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 30000,
            successThreshold = 3
    )
    @Fallback(fallbackMethod = "fallbackProduceAccountingEvent")
    public CompletionStage<Void> produceAccountingEvent(AccountingRequestDto request) {
        try {
            totalRequestsCounter.increment();

            if (!checkAndUpdateTpsLimit()) {
                tpsExceededCounter.increment();
                LOG.warnf("TPS limit exceeded (%d TPS). SessionId: %s - request will be processed but may experience delays",
                        maxTps, request.sessionId());
            }

            String partitionKey = String.format("%s-%s", request.sessionId(), request.nasIP());

            if (LOG.isDebugEnabled()) {
                LOG.debugf("Producing accounting event - SessionId: %s, NasIP: %s, Action: %s, CurrentTPS: %d",
                        request.sessionId(), request.nasIP(), request.actionType(), currentTps.get());
            }
            var metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(partitionKey)
                    .build();
            CompletableFuture<Void> future = new CompletableFuture<>();

            var message = Message.of(request)
                    .addMetadata(metadata)
                    .withAck(() -> {
                        consecutiveFailures.set(0);
                        future.complete(null);
                        return CompletableFuture.completedFuture(null);
                    })
                    .withNack(throwable -> {
                        long failures = consecutiveFailures.incrementAndGet();
                        failureCounter.increment();
                        LOG.errorf("Failed accounting event (consecutive: %d): %s", failures, throwable.getMessage());
                        future.completeExceptionally(throwable);
                        return CompletableFuture.completedFuture(null);
                    });

            accountingEmitter.send(message);
            return future;

        } catch (Exception e) {
            long failures = consecutiveFailures.incrementAndGet();
            failureCounter.increment();
            LOG.errorf(e, "Error producing accounting event (consecutive: %d): %s", failures, request.sessionId());
            return CompletableFuture.failedFuture(e);
        }

    }

    /**
     * Checks and updates the TPS counter using a sliding window approach.
     * This method is lock-free and uses atomic operations for thread safety.
     *
     * @return true if within TPS limit, false if TPS limit exceeded
     */
    private boolean checkAndUpdateTpsLimit() {
        long now = System.currentTimeMillis();
        long windowStart = windowStartTime.get();

        if (now - windowStart >= TPS_WINDOW_SIZE_MS) {
            long requests = currentWindowRequests.getAndSet(1);
            windowStartTime.set(now);
            currentTps.set(requests);
            return true;
        }

        long requests = currentWindowRequests.incrementAndGet();
        currentTps.set(requests);
        return requests <= maxTps;
    }

    /**
     * Returns the current TPS rate for monitoring purposes.
     *
     * @return current transactions per second
     */
    public long getCurrentTps() {
        return currentTps.get();
    }

    /**
     * Returns the maximum configured TPS limit.
     *
     * @return maximum TPS limit
     */
    public int getMaxTps() {
        return maxTps;
    }

    /**
     * Checks if the current TPS is within the configured limit.
     *
     * @return true if within limit, false otherwise
     */
    public boolean isWithinTpsLimit() {
        return currentTps.get() <= maxTps;
    }

    /**
     * Fallback method for circuit breaker - provides alternative path when Kafka is unavailable
     * Logs failure details without blocking the caller
     */
    public CompletionStage<Void> fallbackProduceAccountingEvent(AccountingRequestDto request) {
        fallbackCounter.increment();
        long failures = consecutiveFailures.get();

        LOG.warnf("Circuit breaker activated - Fallback for accounting event - SessionId: %s, NasIP: %s, Action: %s (consecutive failures: %d)",
                request.sessionId(), request.nasIP(), request.actionType(), failures);
        return CompletableFuture.completedFuture(null);
    }

}
