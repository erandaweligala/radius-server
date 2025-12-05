package com.csg.airtel.aaa4j.external.client;

import com.csg.airtel.aaa4j.application.config.WebClientProvider;
import com.csg.airtel.aaa4j.common.constant.AuthServiceConstants;
import com.csg.airtel.aaa4j.common.util.TraceIdGenerator;
import com.csg.airtel.aaa4j.domain.model.UserDetails;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@ApplicationScoped
public class AuthManagementServiceClient {

    private final WebClientProvider webClientProvider;
    private static final Logger logger = LoggerFactory.getLogger(AuthManagementServiceClient.class);

    @Inject
    public AuthManagementServiceClient(WebClientProvider webClientProvider) {
        this.webClientProvider = webClientProvider;
    }

    @ConfigProperty(name = "auth.service.url")
    String authServiceUrl; // Configurable endpoint

    public UserDetails authenticate(String username, String password, String chapChallenge, String chapPassword, String nasIpAddress)  throws InterruptedException {
        WebClient client = webClientProvider.getClient();
        CompletableFuture<UserDetails> future = new CompletableFuture<>();

        String traceId = MDC.get(AuthServiceConstants.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIdGenerator.generateTraceId();
            MDC.put(AuthServiceConstants.TRACE_ID, traceId);
        }

        // Build JSON body
        JsonObject body = new JsonObject();
        body.put("username", username);
        if (password != null && !password.isBlank()) {
            body.put("password", password);
        }
        if (chapChallenge != null && !chapChallenge.isBlank()) {
            body.put("chapChallenge", chapChallenge);
        }
        if (chapPassword != null && !chapPassword.isBlank()) {
            body.put("chapPassword", chapPassword);
        }
        if (nasIpAddress != null && !nasIpAddress.isBlank()) {
            body.put("nasIpAddress", nasIpAddress);
        }

        // Make POST request with JSON body
        client.postAbs(authServiceUrl)
                .putHeader(AuthServiceConstants.HEADER_TRACE_ID, traceId)
                .putHeader(AuthServiceConstants.HEADER_USER_NAME, username)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body, ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        JsonObject json = response.bodyAsJsonObject();
                        UserDetails user = json.mapTo(UserDetails.class);
                        future.complete(user);
                    } else {
                        future.completeExceptionally(ar.cause());
                    }
                });

        try {
            return future.get(); // Blocking, you can add timeout if needed
        } catch (ExecutionException e) {
            logger.error("[{}] {}: {}", traceId, AuthServiceConstants.MSG_INTERNAL_ERROR, e.getMessage(), e);
            return new UserDetails(username, false, false, false, null);
        } finally {
            MDC.remove(AuthServiceConstants.TRACE_ID);
        }
    }

}
