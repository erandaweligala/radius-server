package com.csg.airtel.aaa4j.aspect;

import jakarta.interceptor.InterceptorBinding;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Circuit breaker interceptor binding annotation.
 *
 * Applies circuit breaker pattern to methods, providing:
 * - Automatic failure detection and circuit opening
 * - Success/failure recording
 * - Failed message storage for retry
 *
 * Usage:
 * <pre>
 * {@code
 * @CircuitBreaker
 * public CompletionStage<Void> produceMessage(Request request) {
 *     // method implementation
 * }
 * }
 * </pre>
 *
 * Performance: Zero-overhead when circuit is closed (single atomic read).
 */
@InterceptorBinding
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface CircuitBreaker {
}
