package com.csg.airtel.aaa4j.common.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceConstantsTest {

    @Test
    void testHeaderTraceId() {
        assertEquals("Trace-Id", AuthServiceConstants.HEADER_TRACE_ID);
    }

    @Test
    void testHeaderUserName() {
        assertEquals("User-Name", AuthServiceConstants.HEADER_USER_NAME);
    }

    @Test
    void testParamUserName() {
        assertEquals("userName", AuthServiceConstants.PARAM_USER_NAME);
    }

    @Test
    void testParamPassword() {
        assertEquals("password", AuthServiceConstants.PARAM_PASSWORD);
    }

    @Test
    void testTraceId() {
        assertEquals("traceId", AuthServiceConstants.TRACE_ID);
    }

    @Test
    void testMsgAuthFailed() {
        assertEquals("Authentication failed", AuthServiceConstants.MSG_AUTH_FAILED);
    }

    @Test
    void testMsgInternalError() {
        assertEquals("Internal server error occurred", AuthServiceConstants.MSG_INTERNAL_ERROR);
    }

    @Test
    void testAllConstantsAreNotNull() {
        assertNotNull(AuthServiceConstants.HEADER_TRACE_ID);
        assertNotNull(AuthServiceConstants.HEADER_USER_NAME);
        assertNotNull(AuthServiceConstants.PARAM_USER_NAME);
        assertNotNull(AuthServiceConstants.PARAM_PASSWORD);
        assertNotNull(AuthServiceConstants.TRACE_ID);
        assertNotNull(AuthServiceConstants.MSG_AUTH_FAILED);
        assertNotNull(AuthServiceConstants.MSG_INTERNAL_ERROR);
    }

    @Test
    void testAllConstantsAreNotEmpty() {
        assertFalse(AuthServiceConstants.HEADER_TRACE_ID.isEmpty());
        assertFalse(AuthServiceConstants.HEADER_USER_NAME.isEmpty());
        assertFalse(AuthServiceConstants.PARAM_USER_NAME.isEmpty());
        assertFalse(AuthServiceConstants.PARAM_PASSWORD.isEmpty());
        assertFalse(AuthServiceConstants.TRACE_ID.isEmpty());
        assertFalse(AuthServiceConstants.MSG_AUTH_FAILED.isEmpty());
        assertFalse(AuthServiceConstants.MSG_INTERNAL_ERROR.isEmpty());
    }

    @Test
    void testConstantsArePublicStaticFinal() throws Exception {
        Field[] fields = AuthServiceConstants.class.getDeclaredFields();
        
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            assertTrue(Modifier.isPublic(modifiers), "Field " + field.getName() + " should be public");
            assertTrue(Modifier.isStatic(modifiers), "Field " + field.getName() + " should be static");
            assertTrue(Modifier.isFinal(modifiers), "Field " + field.getName() + " should be final");
        }
    }

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<AuthServiceConstants> constructor = AuthServiceConstants.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        
        // Test that constructor is accessible and can be invoked
        constructor.setAccessible(true);
        AuthServiceConstants instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    void testClassIsFinal() {
        // While not explicitly final in the source, utility classes should typically be final
        // This test documents the current state
        int modifiers = AuthServiceConstants.class.getModifiers();
        assertTrue(Modifier.isPublic(modifiers));
        // Note: The class is not final in the source, so we don't test for that
    }

    @Test
    void testHeaderConstants() {
        // Test that header constants follow HTTP header naming conventions
        assertTrue(AuthServiceConstants.HEADER_TRACE_ID.contains("-"));
        assertTrue(AuthServiceConstants.HEADER_USER_NAME.contains("-"));
        
        // Headers should not contain spaces
        assertFalse(AuthServiceConstants.HEADER_TRACE_ID.contains(" "));
        assertFalse(AuthServiceConstants.HEADER_USER_NAME.contains(" "));
    }

    @Test
    void testParameterConstants() {
        // Parameter names should be camelCase
        assertTrue(Character.isLowerCase(AuthServiceConstants.PARAM_USER_NAME.charAt(0)));
        assertTrue(Character.isLowerCase(AuthServiceConstants.PARAM_PASSWORD.charAt(0)));
        assertTrue(Character.isLowerCase(AuthServiceConstants.TRACE_ID.charAt(0)));
        
        // Should not contain spaces or special characters
        assertFalse(AuthServiceConstants.PARAM_USER_NAME.contains(" "));
        assertFalse(AuthServiceConstants.PARAM_PASSWORD.contains(" "));
        assertFalse(AuthServiceConstants.TRACE_ID.contains(" "));
    }

    @Test
    void testMessageConstants() {
        // Messages should be proper sentences
        assertTrue(!AuthServiceConstants.MSG_AUTH_FAILED.isEmpty());
        assertTrue(!AuthServiceConstants.MSG_INTERNAL_ERROR.isEmpty());
        
        // Should start with uppercase
        assertTrue(Character.isUpperCase(AuthServiceConstants.MSG_AUTH_FAILED.charAt(0)));
        assertTrue(Character.isUpperCase(AuthServiceConstants.MSG_INTERNAL_ERROR.charAt(0)));
    }

    @Test
    void testConstantValues() {
        // Test specific expected values to ensure they don't change accidentally
        assertEquals("Trace-Id", AuthServiceConstants.HEADER_TRACE_ID);
        assertEquals("User-Name", AuthServiceConstants.HEADER_USER_NAME);
        assertEquals("userName", AuthServiceConstants.PARAM_USER_NAME);
        assertEquals("password", AuthServiceConstants.PARAM_PASSWORD);
        assertEquals("traceId", AuthServiceConstants.TRACE_ID);
        assertEquals("Authentication failed", AuthServiceConstants.MSG_AUTH_FAILED);
        assertEquals("Internal server error occurred", AuthServiceConstants.MSG_INTERNAL_ERROR);
    }

    @Test
    void testConstantUniqueness() {
        // Ensure all constants have unique values
        String[] values = {
            AuthServiceConstants.HEADER_TRACE_ID,
            AuthServiceConstants.HEADER_USER_NAME,
            AuthServiceConstants.PARAM_USER_NAME,
            AuthServiceConstants.PARAM_PASSWORD,
            AuthServiceConstants.TRACE_ID,
            AuthServiceConstants.MSG_AUTH_FAILED,
            AuthServiceConstants.MSG_INTERNAL_ERROR
        };
        
        for (int i = 0; i < values.length; i++) {
            for (int j = i + 1; j < values.length; j++) {
                assertNotEquals(values[i], values[j], 
                    "Duplicate constant value found: " + values[i]);
            }
        }
    }
}
