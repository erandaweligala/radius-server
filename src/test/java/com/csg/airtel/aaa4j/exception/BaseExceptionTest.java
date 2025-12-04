package com.csg.airtel.aaa4j.exception;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseExceptionTest {

    @Test
    void testBaseException_Constructor() {
        String message = "Test error message";
        String description = "SERVICE";
        Response.Status httpStatus = Response.Status.BAD_REQUEST;
        String responseCode = "E1001";
        StackTraceElement[] stackTrace = new StackTraceElement[]{
                new StackTraceElement("TestClass", "testMethod", "TestClass.java", 10)
        };

        BaseException exception = new BaseException(message, description, httpStatus, responseCode, stackTrace);

        assertEquals(message, exception.getMessage());
        assertEquals(description, exception.getDescription());
        assertEquals(httpStatus, exception.getHttpStatus());
        assertEquals(responseCode, exception.getResponseCode());
        assertArrayEquals(stackTrace, exception.getStackTraceElements());
    }

    @Test
    void testBaseException_GettersAndSetters() {
        String message = "Another test message";
        String description = "CONTROLLER";
        Response.Status httpStatus = Response.Status.INTERNAL_SERVER_ERROR;
        String responseCode = "E1000";
        StackTraceElement[] stackTrace = new StackTraceElement[]{
                new StackTraceElement("AnotherClass", "anotherMethod", "AnotherClass.java", 25)
        };

        BaseException exception = new BaseException(message, description, httpStatus, responseCode, stackTrace);

        assertNotNull(exception.getDescription());
        assertNotNull(exception.getHttpStatus());
        assertNotNull(exception.getResponseCode());
        assertNotNull(exception.getStackTraceElements());
        
        assertEquals("CONTROLLER", exception.getDescription());
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR, exception.getHttpStatus());
        assertEquals("E1000", exception.getResponseCode());
        assertEquals(1, exception.getStackTraceElements().length);
    }

    @Test
    void testBaseException_ToString() {
        String message = "Test toString message";
        String description = "DATABASE";
        Response.Status httpStatus = Response.Status.NOT_FOUND;
        String responseCode = "E1002";
        StackTraceElement[] stackTrace = new StackTraceElement[]{
                new StackTraceElement("DatabaseClass", "queryMethod", "DatabaseClass.java", 50)
        };

        BaseException exception = new BaseException(message, description, httpStatus, responseCode, stackTrace);
        String toStringResult = exception.toString();

        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("BaseException"));
        assertTrue(toStringResult.contains(message));
        assertTrue(toStringResult.contains(description));
        assertTrue(toStringResult.contains(httpStatus.toString()));
        assertTrue(toStringResult.contains(responseCode));
        assertTrue(toStringResult.contains("DatabaseClass"));
    }

    @Test
    void testBaseException_WithNullValues() {
        BaseException exception = new BaseException(null, null, null, null, null);

        assertNull(exception.getMessage());
        assertNull(exception.getDescription());
        assertNull(exception.getHttpStatus());
        assertNull(exception.getResponseCode());
        assertNull(exception.getStackTraceElements());
    }

    @Test
    void testBaseException_WithEmptyStackTrace() {
        String message = "Empty stack trace test";
        String description = "SERVICE";
        Response.Status httpStatus = Response.Status.BAD_REQUEST;
        String responseCode = "E1001";
        StackTraceElement[] emptyStackTrace = new StackTraceElement[0];

        BaseException exception = new BaseException(message, description, httpStatus, responseCode, emptyStackTrace);

        assertEquals(0, exception.getStackTraceElements().length);
        assertNotNull(exception.toString());
    }

    @Test
    void testBaseException_InheritanceFromRuntimeException() {
        BaseException exception = new BaseException("test", "SERVICE", Response.Status.BAD_REQUEST, "E1001", null);
        
        assertTrue(exception instanceof RuntimeException);
        assertTrue(exception instanceof Exception);
        assertTrue(exception instanceof Throwable);
    }
}
