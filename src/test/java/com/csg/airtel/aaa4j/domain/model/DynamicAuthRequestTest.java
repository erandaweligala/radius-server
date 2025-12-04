package com.csg.airtel.aaa4j.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class DynamicAuthRequestTest {

    static Stream<Arguments> provideAuthRequestData() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("testuser", "testpass", "NAS-001"),
                org.junit.jupiter.params.provider.Arguments.of("admin@company.com", "SecurePass123!", "RADIUS-NAS-MAIN-001"),
                org.junit.jupiter.params.provider.Arguments.of("user@domain.com", "P@ssw0rd!#$%", "NAS-001_MAIN-SERVER")
        );
    }

    @ParameterizedTest
    @MethodSource("provideAuthRequestData")
    void testDynamicAuthRequestFields(String username, String password, String nasIdentifier) {
        // Act
        DynamicAuthRequest request = new DynamicAuthRequest(username, password, nasIdentifier);

        // Assert
        assertEquals(username, request.getUsername());
        assertEquals(password, request.getPassword());
        assertEquals(nasIdentifier, request.getNasIdentifier());
    }

    @Test
    void testParameterizedConstructor_WithNullValues() {
        DynamicAuthRequest request = new DynamicAuthRequest(null, null, null);
        
        assertNull(request.getUsername());
        assertNull(request.getPassword());
        assertNull(request.getNasIdentifier());
    }

    @Test
    void testParameterizedConstructor_WithEmptyValues() {
        DynamicAuthRequest request = new DynamicAuthRequest("", "", "");
        
        assertEquals("", request.getUsername());
        assertEquals("", request.getPassword());
        assertEquals("", request.getNasIdentifier());
    }

    @Test
    void testSettersAndGetters() {
        DynamicAuthRequest request = new DynamicAuthRequest("initial", "initial", "initial");
        
        String newUsername = "newuser";
        String newPassword = "newpass";
        String newNasIdentifier = "NAS-002";
        
        request.setUsername(newUsername);
        request.setPassword(newPassword);
        request.setNasIdentifier(newNasIdentifier);
        
        assertEquals(newUsername, request.getUsername());
        assertEquals(newPassword, request.getPassword());
        assertEquals(newNasIdentifier, request.getNasIdentifier());
    }

    @Test
    void testSetUsername_WithNull() {
        DynamicAuthRequest request = new DynamicAuthRequest("user", "pass", "nas");
        
        request.setUsername(null);
        
        assertNull(request.getUsername());
        assertEquals("pass", request.getPassword()); // Other fields unchanged
        assertEquals("nas", request.getNasIdentifier());
    }

    @Test
    void testSetPassword_WithNull() {
        DynamicAuthRequest request = new DynamicAuthRequest("user", "pass", "nas");
        
        request.setPassword(null);
        
        assertEquals("user", request.getUsername()); // Other fields unchanged
        assertNull(request.getPassword());
        assertEquals("nas", request.getNasIdentifier());
    }

    @Test
    void testSetNasIdentifier_WithNull() {
        DynamicAuthRequest request = new DynamicAuthRequest("user", "pass", "nas");
        
        request.setNasIdentifier(null);
        
        assertEquals("user", request.getUsername()); // Other fields unchanged
        assertEquals("pass", request.getPassword());
        assertNull(request.getNasIdentifier());
    }

    @Test
    void testSetUsername_WithEmptyString() {
        DynamicAuthRequest request = new DynamicAuthRequest("user", "pass", "nas");
        
        request.setUsername("");
        
        assertEquals("", request.getUsername());
    }

    @Test
    void testSetPassword_WithEmptyString() {
        DynamicAuthRequest request = new DynamicAuthRequest("user", "pass", "nas");
        
        request.setPassword("");
        
        assertEquals("", request.getPassword());
    }

    @Test
    void testSetNasIdentifier_WithEmptyString() {
        DynamicAuthRequest request = new DynamicAuthRequest("user", "pass", "nas");
        
        request.setNasIdentifier("");
        
        assertEquals("", request.getNasIdentifier());
    }

    @Test
    void testLongValues() {
        String longUsername = "very_long_username_with_many_characters_that_exceeds_normal_length";
        String longPassword = "very_long_password_with_many_characters_and_special_symbols_!@#$%^&*()";
        String longNasIdentifier = "very_long_nas_identifier_with_many_characters_and_numbers_12345";
        
        DynamicAuthRequest request = new DynamicAuthRequest(longUsername, longPassword, longNasIdentifier);
        
        assertEquals(longUsername, request.getUsername());
        assertEquals(longPassword, request.getPassword());
        assertEquals(longNasIdentifier, request.getNasIdentifier());
    }

    @Test
    void testNumericValues() {
        String numericUsername = "123456789";
        String numericPassword = "987654321";
        String numericNasIdentifier = "111222333";
        
        DynamicAuthRequest request = new DynamicAuthRequest(numericUsername, numericPassword, numericNasIdentifier);
        
        assertEquals(numericUsername, request.getUsername());
        assertEquals(numericPassword, request.getPassword());
        assertEquals(numericNasIdentifier, request.getNasIdentifier());
    }

    @Test
    void testSingleCharacterValues() {
        String singleCharUsername = "a";
        String singleCharPassword = "b";
        String singleCharNasIdentifier = "c";
        
        DynamicAuthRequest request = new DynamicAuthRequest(singleCharUsername, singleCharPassword, singleCharNasIdentifier);
        
        assertEquals(singleCharUsername, request.getUsername());
        assertEquals(singleCharPassword, request.getPassword());
        assertEquals(singleCharNasIdentifier, request.getNasIdentifier());
    }
}
