package com.csg.airtel.aaa4j.domain.producer;

import com.csg.airtel.aaa4j.aspect.CircuitBreaker;
import com.csg.airtel.aaa4j.domain.failurehandling.PublishFailureHandler;
import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;


import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.quarkus.arc.ComponentsProvider.LOG;

@ApplicationScoped
public class RadiusAccountingProducer {

    Emitter<AccountingRequestDto> accountingEmitter;

    @Inject
    PublishFailureHandler failureHandler;

    @Inject
    public RadiusAccountingProducer(@Channel("accounting-events") Emitter<AccountingRequestDto> accountingEmitter) {
        this.accountingEmitter = accountingEmitter;
    }

    /**
     * Produces accounting event to Kafka with automatic circuit breaker protection.
     *
     * The @CircuitBreaker annotation provides:
     * - Automatic circuit state checking (zero-overhead when closed)
     * - Success/failure recording
     * - Failed message storage for retry
     * - No performance impact on happy path
     */
    @CircuitBreaker
    public CompletionStage<Void> produceAccountingEvent(AccountingRequestDto request) {
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
                    future.complete(null);
                    if (LOG.isDebugEnabled()) {
                        LOG.debugf("Successfully published accounting event: session=%s",
                            request.sessionId());
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .withNack(throwable -> {
                    future.completeExceptionally(throwable);
                    return CompletableFuture.completedFuture(null);
                });

        accountingEmitter.send(message);
        return future;
    }

}
