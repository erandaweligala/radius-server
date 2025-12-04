package com.csg.airtel.aaa4j.domain.producer;

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
    // todo need to improve performance and any fail case how to handle pls give me best solution
    Emitter<AccountingRequestDto> accountingEmitter;

    @Inject
    public RadiusAccountingProducer(@Channel("accounting-events") Emitter<AccountingRequestDto> accountingEmitter) {
        this.accountingEmitter = accountingEmitter;
    }

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
                        future.complete(null);
                        return CompletableFuture.completedFuture(null);
                    })
                    .withNack(throwable -> {
                        LOG.errorf("Failed accounting event: %s", throwable.getMessage());
                        future.completeExceptionally(throwable);
                        return CompletableFuture.completedFuture(null);
                    });

            accountingEmitter.send(message);
            return future;

        } catch (Exception e) {
            LOG.errorf(e, "Error producing accounting event: %s", request.sessionId());
            return CompletableFuture.failedFuture(e);
        }

    }

}
