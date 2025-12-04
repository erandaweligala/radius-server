package com.csg.airtel.aaa4j.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserRequestTest {

    @Test
    void testDefaultConstructor() {
        UserRequest userRequest = new UserRequest();
        
        assertNull(userRequest.getUsername());
        assertNull(userRequest.getPassword());
        assertNotNull(userRequest.getAllowedSsids());
        assertTrue(userRequest.getAllowedSsids().isEmpty());
    }

    @Test
    void testParameterizedConstructor() {
        String username = "testuser";
        String password = "testpass";
        List<String> allowedSsids = Arrays.asList("SSID1", "SSID2", "SSID3");
        
        UserRequest userRequest = new UserRequest(username, password, allowedSsids);
        
        assertEquals(username, userRequest.getUsername());
        assertEquals(password, userRequest.getPassword());
        assertEquals(allowedSsids, userRequest.getAllowedSsids());
        assertEquals(3, userRequest.getAllowedSsids().size());
    }

    @Test
    void testParameterizedConstructor_WithNullValues() {
        UserRequest userRequest = new UserRequest(null, null, null);
        
        assertNull(userRequest.getUsername());
        assertNull(userRequest.getPassword());
        assertNotNull(userRequest.getAllowedSsids());
        assertTrue(userRequest.getAllowedSsids().isEmpty());
    }

    @Test
    void testParameterizedConstructor_WithEmptyList() {
        List<String> emptyList = Collections.emptyList();
        UserRequest userRequest = new UserRequest("user", "pass", emptyList);
        
        assertEquals("user", userRequest.getUsername());
        assertEquals("pass", userRequest.getPassword());
        assertEquals(emptyList, userRequest.getAllowedSsids());
        assertTrue(userRequest.getAllowedSsids().isEmpty());
    }

    @Test
    void testSettersAndGetters() {
        UserRequest userRequest = new UserRequest();
        
        String username = "newuser";
        String password = "newpass";
        List<String> allowedSsids = Arrays.asList("WiFi1", "WiFi2");
        
        userRequest.setUsername(username);
        userRequest.setPassword(password);
        userRequest.setAllowedSsids(allowedSsids);
        
        assertEquals(username, userRequest.getUsername());
        assertEquals(password, userRequest.getPassword());
        assertEquals(allowedSsids, userRequest.getAllowedSsids());
    }

    @Test
    void testSetUsername_WithNull() {
        UserRequest userRequest = new UserRequest("user", "pass", null);
        
        userRequest.setUsername(null);
        
        assertNull(userRequest.getUsername());
        assertEquals("pass", userRequest.getPassword()); // Other fields unchanged
    }

    @Test
    void testSetPassword_WithNull() {
        UserRequest userRequest = new UserRequest("user", "pass", null);
        
        userRequest.setPassword(null);
        
        assertEquals("user", userRequest.getUsername()); // Other fields unchanged
        assertNull(userRequest.getPassword());
    }

    @Test
    void testSetAllowedSsids_WithNull() {
        UserRequest userRequest = new UserRequest("user", "pass", Arrays.asList("SSID1"));
        
        userRequest.setAllowedSsids(null);
        
        assertNotNull(userRequest.getAllowedSsids());
        assertTrue(userRequest.getAllowedSsids().isEmpty());
    }

    @Test
    void testSetUsername_WithEmptyString() {
        UserRequest userRequest = new UserRequest();
        
        userRequest.setUsername("");
        
        assertEquals("", userRequest.getUsername());
    }

    @Test
    void testSetPassword_WithEmptyString() {
        UserRequest userRequest = new UserRequest();
        
        userRequest.setPassword("");
        
        assertEquals("", userRequest.getPassword());
    }

    @Test
    void testGetAllowedSsids_ReturnsEmptyListWhenNull() {
        UserRequest userRequest = new UserRequest("user", "pass", null);
        
        List<String> ssids = userRequest.getAllowedSsids();
        
        assertNotNull(ssids);
        assertTrue(ssids.isEmpty());
    }

    @Test
    void testGetAllowedSsids_ReturnsActualListWhenNotNull() {
        List<String> originalSsids = Arrays.asList("SSID1", "SSID2", "SSID3");
        UserRequest userRequest = new UserRequest("user", "pass", originalSsids);
        
        List<String> retrievedSsids = userRequest.getAllowedSsids();
        
        assertEquals(originalSsids, retrievedSsids);
        assertEquals(3, retrievedSsids.size());
        assertTrue(retrievedSsids.contains("SSID1"));
        assertTrue(retrievedSsids.contains("SSID2"));
        assertTrue(retrievedSsids.contains("SSID3"));
    }

    @Test
    void testAllowedSsids_WithSingleItem() {
        List<String> singleSsid = Arrays.asList("OnlySSID");
        UserRequest userRequest = new UserRequest("user", "pass", singleSsid);
        
        assertEquals(1, userRequest.getAllowedSsids().size());
        assertEquals("OnlySSID", userRequest.getAllowedSsids().get(0));
    }

    @Test
    void testAllowedSsids_WithDuplicates() {
        List<String> ssidsWithDuplicates = Arrays.asList("SSID1", "SSID2", "SSID1", "SSID3");
        UserRequest userRequest = new UserRequest("user", "pass", ssidsWithDuplicates);
        
        assertEquals(4, userRequest.getAllowedSsids().size()); // Should preserve duplicates
        assertEquals(ssidsWithDuplicates, userRequest.getAllowedSsids());
    }

    @Test
    void testAllowedSsids_WithEmptyStrings() {
        List<String> ssidsWithEmpty = Arrays.asList("SSID1", "", "SSID2", null);
        UserRequest userRequest = new UserRequest("user", "pass", ssidsWithEmpty);
        
        assertEquals(4, userRequest.getAllowedSsids().size());
        assertTrue(userRequest.getAllowedSsids().contains(""));
        assertTrue(userRequest.getAllowedSsids().contains(null));
    }

    @Test
    void testCompleteUserRequest() {
        String username = "admin";
        String password = "admin123";
        List<String> allowedSsids = Arrays.asList("AdminWiFi", "GuestWiFi", "SecureWiFi");
        
        UserRequest userRequest = new UserRequest(username, password, allowedSsids);
        
        // Verify all fields are set correctly
        assertEquals(username, userRequest.getUsername());
        assertEquals(password, userRequest.getPassword());
        assertEquals(allowedSsids.size(), userRequest.getAllowedSsids().size());
        
        for (String ssid : allowedSsids) {
            assertTrue(userRequest.getAllowedSsids().contains(ssid));
        }
    }
}
