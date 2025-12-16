package com.csg.airtel.aaa4j.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserDetailsTest {

    @Test
    void testDefaultConstructor() {
        UserDetails userDetails = new UserDetails();
        
        assertNull(userDetails.getUsername());
        assertFalse(userDetails.getIsAuthorized());
        assertFalse(userDetails.getIsActive());
        assertNotNull(userDetails.getAttributes());
        assertTrue(userDetails.getAttributes().isEmpty());
    }

    @Test
    void testParameterizedConstructor() {
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("role", "admin");
        attributes.put("department", "IT");
        
        UserDetails userDetails = new UserDetails("testuser", true, true, true,null, attributes);
        
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.getIsAuthorized());
        assertTrue(userDetails.getIsActive());
        assertEquals(attributes, userDetails.getAttributes());
        assertEquals("admin", userDetails.getAttributes().get("role"));
        assertEquals("IT", userDetails.getAttributes().get("department"));
    }

    @Test
    void testParameterizedConstructor_WithNullAttributes() {
        UserDetails userDetails = new UserDetails("testuser", true, false,true,null,  null);
        
        assertEquals("testuser", userDetails.getUsername());
        assertTrue(userDetails.getIsAuthorized());
        assertFalse(userDetails.getIsActive());
        assertNotNull(userDetails.getAttributes());
        assertTrue(userDetails.getAttributes().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        UserDetails userDetails = new UserDetails();
        
        userDetails.setUsername("newuser");
        userDetails.setIsAuthorized(true);
        userDetails.setIsActive(false);
        
        HashMap<String, String> attributes = new HashMap<>();
        attributes.put("key1", "value1");
        userDetails.setAttributes(attributes);
        
        assertEquals("newuser", userDetails.getUsername());
        assertTrue(userDetails.getIsAuthorized());
        assertFalse(userDetails.getIsActive());
        assertEquals(attributes, userDetails.getAttributes());
    }

    @Test
    void testSetAttributes_WithNull() {
        UserDetails userDetails = new UserDetails();
        userDetails.setAttributes(null);
        
        assertNotNull(userDetails.getAttributes());
        assertTrue(userDetails.getAttributes().isEmpty());
    }

    @Test
    void testGetAttributes_ReturnsEmptyMapWhenNull() {
        UserDetails userDetails = new UserDetails("user", false, false,true,null, null);
        
        Map<String, String> attributes = userDetails.getAttributes();
        assertNotNull(attributes);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void testGetAttributes_ReturnsActualMapWhenNotNull() {
        Map<String, String> originalAttributes = new HashMap<>();
        originalAttributes.put("test", "value");
        
        UserDetails userDetails = new UserDetails("user", false, false, true,null,originalAttributes);
        
        Map<String, String> retrievedAttributes = userDetails.getAttributes();
        assertEquals(originalAttributes, retrievedAttributes);
        assertEquals("value", retrievedAttributes.get("test"));
    }

    @Test
    void testBooleanFields() {
        UserDetails userDetails = new UserDetails();
        
        // Test default values
        assertFalse(userDetails.getIsAuthorized());
        assertFalse(userDetails.getIsActive());
        
        // Test setting to true
        userDetails.setIsAuthorized(true);
        userDetails.setIsActive(true);
        assertTrue(userDetails.getIsAuthorized());
        assertTrue(userDetails.getIsActive());
        
        // Test setting back to false
        userDetails.setIsAuthorized(false);
        userDetails.setIsActive(false);
        assertFalse(userDetails.getIsAuthorized());
        assertFalse(userDetails.getIsActive());
    }

}
