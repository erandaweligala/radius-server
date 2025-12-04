package com.csg.airtel.aaa4j.aspect;

import jakarta.interceptor.InvocationContext;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogExecutionInterceptorTest {

    @Mock
    private InvocationContext invocationContext;

    @Mock
    private Logger logger;

    private LogExecutionInterceptor interceptor;
    private TestService testService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        interceptor = new LogExecutionInterceptor();
        testService = new TestService();
        
        Method testMethod = TestService.class.getMethod("testMethod");
        
        when(invocationContext.getTarget()).thenReturn(testService);
        when(invocationContext.getMethod()).thenReturn(testMethod);
        when(invocationContext.getParameters()).thenReturn(new Object[]{"param1"});
    }

    @Test
    void testLogMethod_DebugDisabled() throws Exception {
        try (MockedStatic<Logger> loggerMock = mockStatic(Logger.class);
             MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            
            loggerMock.when(() -> Logger.getLogger(LogExecutionInterceptor.class))
                    .thenReturn(logger);
            when(logger.isDebugEnabled()).thenReturn(false);
            when(invocationContext.proceed()).thenReturn("result");

            Object result = interceptor.logMethod(invocationContext);

            assertEquals("result", result);
            verify(invocationContext, times(1)).proceed();
            verify(logger, never()).debugf(anyString(), any());
        }
    }

    @Test
    void testLogMethod_DebugEnabled_Success() throws Exception {
        try (MockedStatic<Logger> loggerMock = mockStatic(Logger.class);
             MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            
            loggerMock.when(() -> Logger.getLogger(LogExecutionInterceptor.class))
                    .thenReturn(logger);
            when(logger.isDebugEnabled()).thenReturn(true);
            when(invocationContext.proceed()).thenReturn("result");
            
            mdcMock.when(() -> MDC.get("traceId")).thenReturn("trace123");
            mdcMock.when(() -> MDC.get("userName")).thenReturn("testuser");
            mdcMock.when(() -> MDC.get("sessionId")).thenReturn("session123");

            Object result = interceptor.logMethod(invocationContext);

            assertEquals("result", result);
            verify(invocationContext, times(1)).proceed();
        }
    }

    @Test
    void testLogMethod_DebugEnabled_WithNullMDC() throws Exception {
        try (MockedStatic<Logger> loggerMock = mockStatic(Logger.class);
             MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            
            loggerMock.when(() -> Logger.getLogger(LogExecutionInterceptor.class))
                    .thenReturn(logger);
            when(logger.isDebugEnabled()).thenReturn(true);
            when(invocationContext.proceed()).thenReturn("result");
            
            mdcMock.when(() -> MDC.get("traceId")).thenReturn(null);
            mdcMock.when(() -> MDC.get("userName")).thenReturn(null);
            mdcMock.when(() -> MDC.get("sessionId")).thenReturn(null);

            Object result = interceptor.logMethod(invocationContext);

            assertEquals("result", result);
            verify(invocationContext, times(1)).proceed();
        }
    }

    @Test
    void testLogMethod_DebugEnabled_Exception() throws Exception {
        try (MockedStatic<Logger> loggerMock = mockStatic(Logger.class);
             MockedStatic<MDC> mdcMock = mockStatic(MDC.class)) {
            
            loggerMock.when(() -> Logger.getLogger(LogExecutionInterceptor.class))
                    .thenReturn(logger);
            when(logger.isDebugEnabled()).thenReturn(true);
            
            RuntimeException exception = new RuntimeException("Test exception");
            when(invocationContext.proceed()).thenThrow(exception);
            
            mdcMock.when(() -> MDC.get("traceId")).thenReturn("trace123");
            mdcMock.when(() -> MDC.get("userName")).thenReturn("testuser");
            mdcMock.when(() -> MDC.get("sessionId")).thenReturn("session123");

            assertThrows(RuntimeException.class, () -> interceptor.logMethod(invocationContext));

            verify(invocationContext, times(1)).proceed();
            }
    }

    // Test service class for mocking
    public static class TestService {
        public String testMethod() {
            return "result";
        }
    }
}
