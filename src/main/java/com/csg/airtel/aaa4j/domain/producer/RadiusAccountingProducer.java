package com.csg.airtel.aaa4j.domain.producer;

import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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

    private final Emitter<AccountingRequestDto> accountingEmitter;
    private final Counter failureCounter;
    private final Counter fallbackCounter;
    private final AtomicLong consecutiveFailures = new AtomicLong(0);

    @Inject
    public RadiusAccountingProducer(
            @Channel("accounting-events") Emitter<AccountingRequestDto> accountingEmitter,
            MeterRegistry meterRegistry) {
        this.accountingEmitter = accountingEmitter;
        this.failureCounter = meterRegistry.counter("accounting.publish.failures");
        this.fallbackCounter = meterRegistry.counter("accounting.publish.fallback");
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

            String partitionKey = String.format("%s-%s", request.sessionId(), request.nasIP());

            if (LOG.isDebugEnabled()) {
                LOG.debugf("Producing accounting event - SessionId: %s, NasIP: %s, Action: %s",
                        request.sessionId(), request.nasIP(), request.actionType());
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
