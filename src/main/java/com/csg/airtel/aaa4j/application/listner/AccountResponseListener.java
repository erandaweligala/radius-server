package com.csg.airtel.aaa4j.application.listner;

import com.csg.airtel.aaa4j.domain.model.AccountingResponseEvent;
import com.csg.airtel.aaa4j.domain.service.ResponseHandler;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


@ApplicationScoped
public class AccountResponseListener {

    private final Logger logger = Logger.getLogger(AccountResponseListener.class);

   final ResponseHandler accountResponseHandler;

   @Inject
    public AccountResponseListener(ResponseHandler accountResponseHandler) {
        this.accountResponseHandler = accountResponseHandler;
    }

    @Incoming("accounting-resp-events")
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)
    public CompletionStage<Void> consumeWithAck(Message<AccountingResponseEvent> message) {
        AccountingResponseEvent payload = message.getPayload();
        logger.infof("[traceId : %s] Account Event Response Received", payload.eventId());

        // Message is already acked before this method is called
        accountResponseHandler.processAccountingResponse(payload)
                .subscribe().with(
                        result -> logger.infof("Successfully processed event: %s", payload.eventId()),
                        throwable -> logger.errorf(throwable, "Error processing event: %s", payload.eventId())
                );

        return CompletableFuture.completedFuture(null);
    }
}
