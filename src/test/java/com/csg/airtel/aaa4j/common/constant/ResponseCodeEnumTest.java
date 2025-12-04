package com.csg.airtel.aaa4j.common.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResponseCodeEnumTest {

    @Test
    void testExceptionControllerLayer() {
        ResponseCodeEnum code = ResponseCodeEnum.EXCEPTION_CONTROLLER_LAYER;
        
        assertEquals("E1000", code.code());
        assertEquals("Exception Controller Layer Error", code.description());
    }

    @Test
    void testExceptionServiceLayer() {
        ResponseCodeEnum code = ResponseCodeEnum.EXCEPTION_SERVICE_LAYER;
        
        assertEquals("E1001", code.code());
        assertEquals("Exception Service Layer Error", code.description());
    }

    @Test
    void testExceptionDatabaseLayer() {
        ResponseCodeEnum code = ResponseCodeEnum.EXCEPTION_DATABASE_LAYER;
        
        assertEquals("E1002", code.code());
        assertEquals("Exception in Database Layer Error", code.description());
    }

    @Test
    void testUserNotFound() {
        ResponseCodeEnum code = ResponseCodeEnum.USER_NOT_FOUND;
        
        assertEquals("E2001", code.code());
        assertEquals("User Not Found", code.description());
    }

    @Test
    void testAllEnumValues() {
        ResponseCodeEnum[] allValues = ResponseCodeEnum.values();
        
        assertEquals(4, allValues.length);
        
        // Verify all expected values are present
        boolean hasControllerError = false;
        boolean hasServiceError = false;
        boolean hasDatabaseError = false;
        boolean hasUserNotFound = false;
        
        for (ResponseCodeEnum value : allValues) {
            switch (value) {
                case EXCEPTION_CONTROLLER_LAYER:
                    hasControllerError = true;
                    break;
                case EXCEPTION_SERVICE_LAYER:
                    hasServiceError = true;
                    break;
                case EXCEPTION_DATABASE_LAYER:
                    hasDatabaseError = true;
                    break;
                case USER_NOT_FOUND:
                    hasUserNotFound = true;
                    break;
            }
        }
        
        assertTrue(hasControllerError);
        assertTrue(hasServiceError);
        assertTrue(hasDatabaseError);
        assertTrue(hasUserNotFound);
    }

    @Test
    void testValueOf() {
        assertEquals(ResponseCodeEnum.EXCEPTION_CONTROLLER_LAYER, 
                    ResponseCodeEnum.valueOf("EXCEPTION_CONTROLLER_LAYER"));
        assertEquals(ResponseCodeEnum.EXCEPTION_SERVICE_LAYER, 
                    ResponseCodeEnum.valueOf("EXCEPTION_SERVICE_LAYER"));
        assertEquals(ResponseCodeEnum.EXCEPTION_DATABASE_LAYER, 
                    ResponseCodeEnum.valueOf("EXCEPTION_DATABASE_LAYER"));
        assertEquals(ResponseCodeEnum.USER_NOT_FOUND, 
                    ResponseCodeEnum.valueOf("USER_NOT_FOUND"));
    }

    @Test
    void testValueOf_InvalidValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            ResponseCodeEnum.valueOf("INVALID_CODE");
        });
    }

    @Test
    void testCodeUniqueness() {
        ResponseCodeEnum[] allValues = ResponseCodeEnum.values();
        
        // Check that all codes are unique
        for (int i = 0; i < allValues.length; i++) {
            for (int j = i + 1; j < allValues.length; j++) {
                assertNotEquals(allValues[i].code(), allValues[j].code(), 
                    "Duplicate code found: " + allValues[i].code());
            }
        }
    }

    @Test
    void testDescriptionNotEmpty() {
        ResponseCodeEnum[] allValues = ResponseCodeEnum.values();
        
        for (ResponseCodeEnum value : allValues) {
            assertNotNull(value.description());
            assertFalse(value.description().isEmpty());
            assertFalse(value.description().trim().isEmpty());
        }
    }

    @Test
    void testCodeNotEmpty() {
        ResponseCodeEnum[] allValues = ResponseCodeEnum.values();
        
        for (ResponseCodeEnum value : allValues) {
            assertNotNull(value.code());
            assertFalse(value.code().isEmpty());
            assertFalse(value.code().trim().isEmpty());
        }
    }

    @Test
    void testCodeFormat() {
        ResponseCodeEnum[] allValues = ResponseCodeEnum.values();
        
        for (ResponseCodeEnum value : allValues) {
            String code = value.code();
            // All codes should start with 'E' and be followed by digits
            assertTrue(code.matches("E\\d+"), 
                "Code format invalid: " + code);
        }
    }

    @Test
    void testEnumOrdinals() {
        assertEquals(0, ResponseCodeEnum.EXCEPTION_CONTROLLER_LAYER.ordinal());
        assertEquals(1, ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.ordinal());
        assertEquals(2, ResponseCodeEnum.EXCEPTION_DATABASE_LAYER.ordinal());
        assertEquals(3, ResponseCodeEnum.USER_NOT_FOUND.ordinal());
    }

    @Test
    void testEnumName() {
        assertEquals("EXCEPTION_CONTROLLER_LAYER", ResponseCodeEnum.EXCEPTION_CONTROLLER_LAYER.name());
        assertEquals("EXCEPTION_SERVICE_LAYER", ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.name());
        assertEquals("EXCEPTION_DATABASE_LAYER", ResponseCodeEnum.EXCEPTION_DATABASE_LAYER.name());
        assertEquals("USER_NOT_FOUND", ResponseCodeEnum.USER_NOT_FOUND.name());
    }

    @Test
    void testToString() {
        // Enum toString() should return the name by default
        assertEquals("EXCEPTION_CONTROLLER_LAYER", ResponseCodeEnum.EXCEPTION_CONTROLLER_LAYER.toString());
        assertEquals("EXCEPTION_SERVICE_LAYER", ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.toString());
        assertEquals("EXCEPTION_DATABASE_LAYER", ResponseCodeEnum.EXCEPTION_DATABASE_LAYER.toString());
        assertEquals("USER_NOT_FOUND", ResponseCodeEnum.USER_NOT_FOUND.toString());
    }
}
