package com.csg.airtel.aaa4j.domain.failurehandling;

import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles publish failures with circuit breaker pattern and retry mechanism.
 *
 * Features:
 * - Circuit breaker to prevent cascading failures
 * - Async retry with exponential backoff
 * - Dead letter queue for permanent failures
 * - No blocking - maintains high throughput
 */
@ApplicationScoped
public class PublishFailureHandler {

    private static final Logger logger = Logger.getLogger(PublishFailureHandler.class);

    @ConfigProperty(name = "accounting.failure.circuit-breaker.failure-threshold", defaultValue = "5")
    int failureThreshold;

    @ConfigProperty(name = "accounting.failure.circuit-breaker.timeout-seconds", defaultValue = "60")
    int circuitBreakerTimeoutSeconds;

    @ConfigProperty(name = "accounting.failure.max-retry-attempts", defaultValue = "3")
    int maxRetryAttempts;

    @ConfigProperty(name = "accounting.failure.retry-delay-ms", defaultValue = "1000")
    long retryDelayMs;

    @Inject
    FailedMessageStore failedMessageStore;

    @Inject
    @Channel("accounting-events-dlq")
    Emitter<AccountingRequestDto> dlqEmitter;

    @Inject
    @Channel("accounting-events")
    Emitter<AccountingRequestDto> accountingEmitter;

    // Circuit breaker state
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong circuitOpenedAt = new AtomicLong(0);
    private volatile CircuitState circuitState = CircuitState.CLOSED;

    // Metrics
    private final AtomicLong totalFailures = new AtomicLong(0);
    private final AtomicLong totalRetries = new AtomicLong(0);
    private final AtomicLong totalDlqMessages = new AtomicLong(0);
    private final AtomicLong totalRecovered = new AtomicLong(0);

    public enum CircuitState {
        CLOSED,     // Normal operation
        OPEN,       // Failing - store messages for retry
        HALF_OPEN   // Testing if service recovered
    }

    /**
     * Check if circuit breaker allows the operation
     */
    public boolean isCircuitOpen() {
        if (circuitState == CircuitState.CLOSED) {
            return false;
        }

        if (circuitState == CircuitState.OPEN) {
            long openDuration = Duration.between(
                Instant.ofEpochMilli(circuitOpenedAt.get()),
                Instant.now()
            ).getSeconds();

            if (openDuration >= circuitBreakerTimeoutSeconds) {
                logger.info("Circuit breaker transitioning to HALF_OPEN state");
                circuitState = CircuitState.HALF_OPEN;
                return false;
            }
            return true;
        }

        // HALF_OPEN state - allow one request through to test
        return false;
    }

    /**
     * Record a successful publish
     */
    public void recordSuccess() {
        int failures = consecutiveFailures.get();
        if (failures > 0) {
            consecutiveFailures.set(0);
            if (circuitState != CircuitState.CLOSED) {
                logger.info("Circuit breaker closing after successful publish");
                circuitState = CircuitState.CLOSED;
            }
        }
    }

    /**
     * Record a failed publish
     */
    public void recordFailure() {
        totalFailures.incrementAndGet();
        int failures = consecutiveFailures.incrementAndGet();

        if (failures >= failureThreshold && circuitState == CircuitState.CLOSED) {
            logger.warnf("Circuit breaker opening after %d consecutive failures", failures);
            circuitState = CircuitState.OPEN;
            circuitOpenedAt.set(System.currentTimeMillis());
        }
    }

    /**
     * Store failed message for retry
     */
    public void storeFailed(AccountingRequestDto request, String partitionKey, Throwable error) {
        if (failedMessageStore.offer(request, partitionKey)) {
            logger.debugf("Stored failed message for retry: session=%s, error=%s",
                request.sessionId(), error.getMessage());
        } else {
            logger.errorf("Failed message store is full, sending to DLQ: session=%s",
                request.sessionId());
            sendToDlq(request, partitionKey, error);
        }
    }

    /**
     * Retry a failed message
     */
    public CompletableFuture<Boolean> retryMessage(
            FailedMessage failedMessage,
            Emitter<AccountingRequestDto> emitter) {

        if (failedMessage.getRetryCount() >= maxRetryAttempts) {
            logger.warnf("Max retry attempts reached for session=%s, sending to DLQ",
                failedMessage.getRequest().sessionId());
            sendToDlq(failedMessage.getRequest(), failedMessage.getPartitionKey(),
                new Exception("Max retry attempts exceeded"));
            return CompletableFuture.completedFuture(false);
        }

        totalRetries.incrementAndGet();
        failedMessage.incrementRetryCount();

        CompletableFuture<Boolean> future = new CompletableFuture<>();

        try {
            var metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(failedMessage.getPartitionKey())
                    .build();

            var message = Message.of(failedMessage.getRequest())
                    .addMetadata(metadata)
                    .withAck(() -> {
                        totalRecovered.incrementAndGet();
                        recordSuccess();
                        future.complete(true);
                        logger.debugf("Retry successful for session=%s after %d attempts",
                            failedMessage.getRequest().sessionId(), failedMessage.getRetryCount());
                        return CompletableFuture.completedFuture(null);
                    })
                    .withNack(throwable -> {
                        recordFailure();
                        future.complete(false);
                        logger.debugf("Retry failed for session=%s, attempt %d",
                            failedMessage.getRequest().sessionId(), failedMessage.getRetryCount());
                        return CompletableFuture.completedFuture(null);
                    });

            emitter.send(message);

        } catch (Exception e) {
            recordFailure();
            future.complete(false);
            logger.errorf(e, "Error retrying message for session=%s",
                failedMessage.getRequest().sessionId());
        }

        return future;
    }

    /**
     * Send message to dead letter queue
     */
    private void sendToDlq(AccountingRequestDto request, String partitionKey, Throwable error) {
        totalDlqMessages.incrementAndGet();

        try {
            var metadata = OutgoingKafkaRecordMetadata.<String>builder()
                    .withKey(partitionKey)
                    .withHeader("error-message", error.getMessage() != null ?
                        error.getMessage().getBytes() : "Unknown error".getBytes())
                    .withHeader("original-timestamp",
                        String.valueOf(System.currentTimeMillis()).getBytes())
                    .build();

            var message = Message.of(request)
                    .addMetadata(metadata);

            dlqEmitter.send(message);

            logger.warnf("Message sent to DLQ: session=%s, error=%s",
                request.sessionId(), error.getMessage());

        } catch (Exception e) {
            logger.errorf(e, "CRITICAL: Failed to send message to DLQ for session=%s",
                request.sessionId());
        }
    }

    /**
     * Background job to retry failed messages (runs every 5 seconds)
     */
    @Scheduled(every = "5s")
    void retryFailedMessages() {
        if (circuitState == CircuitState.OPEN) {
            logger.debug("Circuit is OPEN, skipping retry");
            return;
        }

        if (failedMessageStore.isEmpty()) {
            return;
        }

        int retried = 0;
        int requeued = 0;
        int maxBatchSize = 10; // Process up to 10 messages per batch

        while (retried < maxBatchSize) {
            FailedMessage failedMessage = failedMessageStore.poll();
            if (failedMessage == null) {
                break;
            }

            // Calculate exponential backoff delay
            long backoffDelay = calculateBackoff(failedMessage.getRetryCount());
            long timeSinceFailure = System.currentTimeMillis() - failedMessage.getFailedAt();

            if (timeSinceFailure < backoffDelay) {
                // Put it back and try later
                failedMessageStore.offer(
                    failedMessage.getRequest(),
                    failedMessage.getPartitionKey()
                );
                requeued++;
                continue;
            }

            // Attempt retry
            retryMessage(failedMessage, accountingEmitter)
                .thenAccept(success -> {
                    if (!success && failedMessage.getRetryCount() < maxRetryAttempts) {
                        // Requeue for another retry
                        failedMessageStore.offer(
                            failedMessage.getRequest(),
                            failedMessage.getPartitionKey()
                        );
                    }
                });

            retried++;
        }

        if (retried > 0 || requeued > 0) {
            logger.debugf("Retry batch complete: retried=%d, requeued=%d, remaining=%d",
                retried, requeued, failedMessageStore.size());
        }
    }

    /**
     * Calculate exponential backoff delay
     */
    private long calculateBackoff(int retryCount) {
        return retryDelayMs * (long) Math.pow(2, Math.min(retryCount, 5));
    }

    /**
     * Get current circuit state
     */
    public CircuitState getCircuitState() {
        return circuitState;
    }

    /**
     * Get metrics
     */
    public PublishMetrics getMetrics() {
        return new PublishMetrics(
            totalFailures.get(),
            totalRetries.get(),
            totalDlqMessages.get(),
            totalRecovered.get(),
            failedMessageStore.size(),
            consecutiveFailures.get(),
            circuitState
        );
    }

    /**
     * Metrics record
     */
    public record PublishMetrics(
        long totalFailures,
        long totalRetries,
        long totalDlqMessages,
        long totalRecovered,
        int queueSize,
        int consecutiveFailures,
        CircuitState circuitState
    ) {}
}
