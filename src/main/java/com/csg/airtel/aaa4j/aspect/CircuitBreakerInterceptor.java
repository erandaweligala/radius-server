package com.csg.airtel.aaa4j.aspect;

import com.csg.airtel.aaa4j.domain.failurehandling.PublishFailureHandler;
import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Interceptor that applies circuit breaker pattern to methods.
 *
 * Performance characteristics:
 * - When circuit is CLOSED: Single atomic read (~1ns overhead)
 * - When circuit is OPEN: Fast-fail path, stores for retry
 * - No blocking operations
 * - Minimal object allocation
 *
 * Supports methods that return CompletionStage<Void> or CompletableFuture<Void>.
 * Method must accept AccountingRequestDto as first parameter for failure handling.
 */
@CircuitBreaker
@Interceptor
public class CircuitBreakerInterceptor {

    private static final Logger LOG = Logger.getLogger(CircuitBreakerInterceptor.class);

    @Inject
    PublishFailureHandler failureHandler;

    @AroundInvoke
    public Object applyCircuitBreaker(InvocationContext ctx) throws Exception {
        // Fast path: circuit closed - just proceed (zero overhead)
        if (!failureHandler.isCircuitOpen()) {
            Object result = ctx.proceed();

            // If method returns CompletionStage, wrap it to record success/failure
            if (result instanceof CompletionStage<?>) {
                return wrapCompletionStage((CompletionStage<?>) result, ctx);
            }

            // For non-async methods, record success immediately
            failureHandler.recordSuccess();
            return result;
        }

        // Circuit is open - fast fail path
        return handleOpenCircuit(ctx);
    }

    /**
     * Wrap CompletionStage to record success/failure without blocking
     */
    private CompletionStage<?> wrapCompletionStage(CompletionStage<?> stage, InvocationContext ctx) {
        return stage
            .whenComplete((result, throwable) -> {
                if (throwable == null) {
                    failureHandler.recordSuccess();
                } else {
                    recordFailureAndStore(ctx, throwable);
                }
            });
    }

    /**
     * Handle circuit open state - store message for retry
     */
    private CompletionStage<Void> handleOpenCircuit(InvocationContext ctx) {
        Object[] params = ctx.getParameters();

        if (params.length > 0 && params[0] instanceof AccountingRequestDto request) {
            String partitionKey = String.format("%s-%s", request.sessionId(), request.nasIP());

            LOG.warnf("Circuit breaker is OPEN, storing message for retry: session=%s",
                request.sessionId());

            failureHandler.storeFailed(request, partitionKey,
                new Exception("Circuit breaker is OPEN"));
        } else {
            LOG.warn("Circuit breaker is OPEN but no AccountingRequestDto found in parameters");
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Record failure and store message for retry (non-blocking)
     */
    private void recordFailureAndStore(InvocationContext ctx, Throwable throwable) {
        failureHandler.recordFailure();

        Object[] params = ctx.getParameters();
        if (params.length > 0 && params[0] instanceof AccountingRequestDto request) {
            String partitionKey = String.format("%s-%s", request.sessionId(), request.nasIP());

            LOG.warnf("Failed to publish accounting event: session=%s, error=%s",
                request.sessionId(), throwable.getMessage());

            failureHandler.storeFailed(request, partitionKey, throwable);
        }
    }
}
