package com.csg.airtel.aaa4j.external.client;

import com.csg.airtel.aaa4j.application.config.WebClientProvider;
import com.csg.airtel.aaa4j.common.constant.AuthServiceConstants;
import com.csg.airtel.aaa4j.common.util.TraceIdGenerator;
import com.csg.airtel.aaa4j.domain.model.UserDetails;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class AuthManagementServiceClient {

    private final WebClientProvider webClientProvider;
    private final com.csg.airtel.aaa4j.domain.service.UserDetailsCacheService cacheService;
    private static final Logger logger = LoggerFactory.getLogger(AuthManagementServiceClient.class);

    @Inject
    public AuthManagementServiceClient(WebClientProvider webClientProvider,
                                        com.csg.airtel.aaa4j.domain.service.UserDetailsCacheService cacheService) {
        this.webClientProvider = webClientProvider;
        this.cacheService = cacheService;
    }

    @ConfigProperty(name = "auth.service.url")
    String authServiceUrl;

    @ConfigProperty(name = "auth.service.timeout", defaultValue = "5000")
    long authServiceTimeout;

    /**
     * Authenticate user with reactive, non-blocking approach
     * Includes caching, fault tolerance: timeout, circuit breaker, retry, and fallback
     */
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500, jitter = 200)
    @CircuitBreaker(
            requestVolumeThreshold = 10,
            failureRatio = 0.5,
            delay = 5000,
            successThreshold = 3
    )
    @Fallback(fallbackMethod = "authenticateFallback")
    public Uni<UserDetails> authenticate(String username, String password, String chapChallenge,
                                          String chapPassword, String nasIpAddress) {

        String traceId = MDC.get(AuthServiceConstants.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIdGenerator.generateTraceId();
            MDC.put(AuthServiceConstants.TRACE_ID, traceId);
        }

        final String finalTraceId = traceId;

        // Try to get from cache first
        return cacheService.get(username)
                .chain(cachedUserDetails -> {
                    if (cachedUserDetails != null) {
                        logger.infof("[%s] Using cached authentication for user: %s", finalTraceId, username);
                        return Uni.createFrom().item(cachedUserDetails);
                    }

                    // Cache miss - authenticate with external service
                    return authenticateFromService(username, password, chapChallenge, chapPassword, nasIpAddress, finalTraceId)
                            .chain(userDetails -> {
                                // Cache the result if successful
                                return cacheService.set(username, userDetails)
                                        .replaceWith(userDetails);
                            });
                });
    }

    /**
     * Authenticate user from external service (called on cache miss)
     */
    private Uni<UserDetails> authenticateFromService(String username, String password, String chapChallenge,
                                                      String chapPassword, String nasIpAddress, String traceId) {

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

        logger.infof("[%s] Authenticating user from service: %s", traceId, username);

        // Get Mutiny WebClient for reactive operations
        WebClient mutinyClient = webClientProvider.getMutinyClient();

        return mutinyClient.postAbs(authServiceUrl)
                .putHeader(AuthServiceConstants.HEADER_TRACE_ID, traceId)
                .putHeader(AuthServiceConstants.HEADER_USER_NAME, username)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(body)
                .onItem().transform(response -> {
                    if (response.statusCode() == 200) {
                        JsonObject json = response.bodyAsJsonObject();
                        UserDetails userDetails = json.mapTo(UserDetails.class);
                        logger.infof("[%s] Authentication successful for user: %s", traceId, username);
                        return userDetails;
                    } else {
                        logger.warnf("[%s] Authentication failed with status: %d for user: %s",
                                traceId, response.statusCode(), username);
                        return new UserDetails(username, false, false, false, null);
                    }
                })
                .onFailure().invoke(throwable ->
                    logger.errorf(throwable, "[%s] Authentication service error for user: %s",
                            traceId, username)
                )
                .onFailure().recoverWithItem(throwable -> {
                    logger.errorf("[%s] Recovering from authentication failure for user: %s",
                            traceId, username);
                    return new UserDetails(username, false, false, false, null);
                });
    }

    /**
     * Fallback method when circuit breaker is open or all retries fail
     */
    private Uni<UserDetails> authenticateFallback(String username, String password, String chapChallenge,
                                                    String chapPassword, String nasIpAddress) {
        String traceId = MDC.get(AuthServiceConstants.TRACE_ID);
        logger.warnf("[%s] Using fallback authentication for user: %s - Auth service unavailable",
                traceId, username);

        // Return unauthorized user details when service is unavailable
        return Uni.createFrom().item(new UserDetails(username, false, false, false, null));
    }

}
