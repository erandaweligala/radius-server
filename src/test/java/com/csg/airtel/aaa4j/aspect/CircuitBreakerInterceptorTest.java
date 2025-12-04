package com.csg.airtel.aaa4j.aspect;

import com.csg.airtel.aaa4j.domain.failurehandling.PublishFailureHandler;
import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import jakarta.interceptor.InvocationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CircuitBreakerInterceptorTest {

    private CircuitBreakerInterceptor interceptor;
    private PublishFailureHandler failureHandler;
    private InvocationContext invocationContext;
    private AccountingRequestDto testRequest;

    @BeforeEach
    void setUp() {
        interceptor = new CircuitBreakerInterceptor();
        failureHandler = mock(PublishFailureHandler.class);
        invocationContext = mock(InvocationContext.class);
        interceptor.failureHandler = failureHandler;

        // Create test request
        testRequest = new AccountingRequestDto(
            "session-123",
            "192.168.1.1",
            "START",
            1000L,
            500L,
            "user@test.com"
        );
    }

    @Test
    void testCircuitClosed_Success_RecordsSuccess() throws Exception {
        // Given: Circuit is closed and method succeeds
        when(failureHandler.isCircuitOpen()).thenReturn(false);
        CompletableFuture<Void> successFuture = CompletableFuture.completedFuture(null);
        when(invocationContext.proceed()).thenReturn(successFuture);
        when(invocationContext.getParameters()).thenReturn(new Object[]{testRequest});

        // When
        Object result = interceptor.applyCircuitBreaker(invocationContext);

        // Then: Success is recorded after completion
        assertTrue(result instanceof CompletionStage);
        CompletionStage<?> stage = (CompletionStage<?>) result;
        stage.toCompletableFuture().get(); // Wait for completion

        verify(failureHandler, times(1)).isCircuitOpen();
        verify(failureHandler, times(1)).recordSuccess();
        verify(failureHandler, never()).recordFailure();
        verify(failureHandler, never()).storeFailed(any(), any(), any());
    }

    @Test
    void testCircuitClosed_Failure_RecordsFailureAndStores() throws Exception {
        // Given: Circuit is closed but method fails
        when(failureHandler.isCircuitOpen()).thenReturn(false);
        RuntimeException testException = new RuntimeException("Kafka publish failed");
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(testException);

        when(invocationContext.proceed()).thenReturn(failedFuture);
        when(invocationContext.getParameters()).thenReturn(new Object[]{testRequest});

        // When
        Object result = interceptor.applyCircuitBreaker(invocationContext);

        // Then: Failure is recorded and message is stored
        assertTrue(result instanceof CompletionStage);
        CompletionStage<?> stage = (CompletionStage<?>) result;

        try {
            stage.toCompletableFuture().get();
            fail("Should have thrown exception");
        } catch (ExecutionException e) {
            assertEquals(testException, e.getCause());
        }

        verify(failureHandler, times(1)).isCircuitOpen();
        verify(failureHandler, times(1)).recordFailure();
        verify(failureHandler, never()).recordSuccess();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(failureHandler, times(1)).storeFailed(
            eq(testRequest),
            keyCaptor.capture(),
            eq(testException)
        );

        String capturedKey = keyCaptor.getValue();
        assertEquals("session-123-192.168.1.1", capturedKey);
    }

    @Test
    void testCircuitOpen_StoresMessageAndReturnsCompletedFuture() throws Exception {
        // Given: Circuit is open
        when(failureHandler.isCircuitOpen()).thenReturn(true);
        when(invocationContext.getParameters()).thenReturn(new Object[]{testRequest});

        // When
        Object result = interceptor.applyCircuitBreaker(invocationContext);

        // Then: Method is not invoked, message is stored, completed future returned
        verify(invocationContext, never()).proceed();
        verify(failureHandler, never()).recordSuccess();
        verify(failureHandler, never()).recordFailure();

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Throwable> errorCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(failureHandler, times(1)).storeFailed(
            eq(testRequest),
            keyCaptor.capture(),
            errorCaptor.capture()
        );

        assertEquals("session-123-192.168.1.1", keyCaptor.getValue());
        assertTrue(errorCaptor.getValue().getMessage().contains("Circuit breaker is OPEN"));

        // Verify it returns a completed future
        assertTrue(result instanceof CompletionStage);
        CompletionStage<?> stage = (CompletionStage<?>) result;
        assertNull(stage.toCompletableFuture().get()); // Should complete without error
    }

    @Test
    void testCircuitOpen_NoAccountingRequestDto_HandlesGracefully() throws Exception {
        // Given: Circuit is open but no AccountingRequestDto in parameters
        when(failureHandler.isCircuitOpen()).thenReturn(true);
        when(invocationContext.getParameters()).thenReturn(new Object[]{"not-a-request"});

        // When
        Object result = interceptor.applyCircuitBreaker(invocationContext);

        // Then: No exception thrown, gracefully handled
        verify(failureHandler, never()).storeFailed(any(), any(), any());
        assertTrue(result instanceof CompletionStage);
        assertNotNull(((CompletionStage<?>) result).toCompletableFuture().get());
    }

    @Test
    void testNonAsyncMethod_RecordsSuccessImmediately() throws Exception {
        // Given: Circuit is closed and method returns non-CompletionStage
        when(failureHandler.isCircuitOpen()).thenReturn(false);
        when(invocationContext.proceed()).thenReturn("simple-result");

        // When
        Object result = interceptor.applyCircuitBreaker(invocationContext);

        // Then: Success is recorded immediately
        assertEquals("simple-result", result);
        verify(failureHandler, times(1)).recordSuccess();
    }

    @Test
    void testPartitionKeyFormat() throws Exception {
        // Given: Various session and NAS IP combinations
        AccountingRequestDto request1 = new AccountingRequestDto(
            "sess-456",
            "10.0.0.1",
            "STOP",
            2000L,
            1000L,
            "admin@test.com"
        );

        when(failureHandler.isCircuitOpen()).thenReturn(true);
        when(invocationContext.getParameters()).thenReturn(new Object[]{request1});

        // When
        interceptor.applyCircuitBreaker(invocationContext);

        // Then: Partition key format is correct
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(failureHandler, times(1)).storeFailed(
            eq(request1),
            keyCaptor.capture(),
            any()
        );

        assertEquals("sess-456-10.0.0.1", keyCaptor.getValue());
    }

    @Test
    void testPerformance_CircuitClosedHappyPath() throws Exception {
        // This test verifies minimal overhead on happy path
        when(failureHandler.isCircuitOpen()).thenReturn(false);
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(invocationContext.proceed()).thenReturn(future);
        when(invocationContext.getParameters()).thenReturn(new Object[]{testRequest});

        // When: Execute multiple times
        int iterations = 1000;
        long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            CompletionStage<?> result = (CompletionStage<?>) interceptor.applyCircuitBreaker(invocationContext);
            result.toCompletableFuture().get();
        }

        long duration = System.nanoTime() - startTime;
        double avgMicroseconds = (duration / iterations) / 1000.0;

        // Then: Average overhead should be minimal (< 10 microseconds per call)
        assertTrue(avgMicroseconds < 10,
            String.format("Average overhead too high: %.2f µs", avgMicroseconds));

        System.out.printf("CircuitBreaker overhead (happy path): %.2f µs per call%n", avgMicroseconds);
    }

    @Test
    void testAsyncCompletionHandling_CompletesAsynchronously() throws Exception {
        // Given: Circuit is closed, method returns async future
        when(failureHandler.isCircuitOpen()).thenReturn(false);
        CompletableFuture<Void> asyncFuture = new CompletableFuture<>();
        when(invocationContext.proceed()).thenReturn(asyncFuture);
        when(invocationContext.getParameters()).thenReturn(new Object[]{testRequest});

        // When
        CompletionStage<?> result = (CompletionStage<?>) interceptor.applyCircuitBreaker(invocationContext);

        // Complete async after some delay
        new Thread(() -> {
            try {
                Thread.sleep(10);
                asyncFuture.complete(null);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();

        // Then: Success recorded after async completion
        result.toCompletableFuture().get();
        verify(failureHandler, times(1)).recordSuccess();
    }
}
