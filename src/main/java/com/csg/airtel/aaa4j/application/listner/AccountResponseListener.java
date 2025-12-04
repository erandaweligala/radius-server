package com.csg.airtel.aaa4j.application.listner;

import com.csg.airtel.aaa4j.domain.model.AccountingResponseEvent;
import com.csg.airtel.aaa4j.domain.service.ResponseHandler;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;


@ApplicationScoped
public class AccountResponseListener {

    private final Logger logger = Logger.getLogger(AccountResponseListener.class);

   final ResponseHandler accountResponseHandler;

   @Inject
    public AccountResponseListener(ResponseHandler accountResponseHandler) {
        this.accountResponseHandler = accountResponseHandler;
    }

    @Incoming("accounting-resp-events")
    public Uni<Void> consumeWithAck(Message<AccountingResponseEvent> message) {
        AccountingResponseEvent payload = message.getPayload();
        logger.infof("[traceId : %s] Account Event Response Received", payload.eventId());

      return  accountResponseHandler.processAccountingResponse(payload).onItem().transformToUni(result -> Uni.createFrom().completionStage(message.ack()))
                .onFailure().recoverWithUni(throwable -> {
                  logger.errorf(throwable, "Error processing event for user: %s | eventType: %s",
                          payload.eventId(), payload.eventType());
                    return Uni.createFrom().completionStage(message.nack(throwable));
                });
    }
}
