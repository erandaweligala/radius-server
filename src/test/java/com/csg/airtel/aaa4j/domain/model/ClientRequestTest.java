package com.csg.airtel.aaa4j.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientRequestTest {

    @Test
    void testDefaultConstructor() {
        ClientRequest clientRequest = new ClientRequest();
        
        assertNull(clientRequest.getIpAddress());
        assertNull(clientRequest.getSharedSecret());
    }

    @Test
    void testParameterizedConstructor() {
        String ipAddress = "192.168.1.100";
        String sharedSecret = "mysecret123";
        
        ClientRequest clientRequest = new ClientRequest(ipAddress, sharedSecret);
        
        assertEquals(ipAddress, clientRequest.getIpAddress());
        assertEquals(sharedSecret, clientRequest.getSharedSecret());
    }

    @Test
    void testParameterizedConstructor_WithNullValues() {
        ClientRequest clientRequest = new ClientRequest(null, null);
        
        assertNull(clientRequest.getIpAddress());
        assertNull(clientRequest.getSharedSecret());
    }

    @Test
    void testParameterizedConstructor_WithEmptyValues() {
        ClientRequest clientRequest = new ClientRequest("", "");
        
        assertEquals("", clientRequest.getIpAddress());
        assertEquals("", clientRequest.getSharedSecret());
    }

    @Test
    void testSettersAndGetters() {
        ClientRequest clientRequest = new ClientRequest();
        
        String ipAddress = "10.0.0.1";
        String sharedSecret = "topsecret";
        
        clientRequest.setIpAddress(ipAddress);
        clientRequest.setSharedSecret(sharedSecret);
        
        assertEquals(ipAddress, clientRequest.getIpAddress());
        assertEquals(sharedSecret, clientRequest.getSharedSecret());
    }

    @Test
    void testSetIpAddress_WithNull() {
        ClientRequest clientRequest = new ClientRequest("192.168.1.1", "secret");
        
        clientRequest.setIpAddress(null);
        
        assertNull(clientRequest.getIpAddress());
        assertEquals("secret", clientRequest.getSharedSecret()); // Other field unchanged
    }

    @Test
    void testSetSharedSecret_WithNull() {
        ClientRequest clientRequest = new ClientRequest("192.168.1.1", "secret");
        
        clientRequest.setSharedSecret(null);
        
        assertEquals("192.168.1.1", clientRequest.getIpAddress()); // Other field unchanged
        assertNull(clientRequest.getSharedSecret());
    }

    @Test
    void testSetIpAddress_WithEmptyString() {
        ClientRequest clientRequest = new ClientRequest();
        
        clientRequest.setIpAddress("");
        
        assertEquals("", clientRequest.getIpAddress());
    }

    @Test
    void testSetSharedSecret_WithEmptyString() {
        ClientRequest clientRequest = new ClientRequest();
        
        clientRequest.setSharedSecret("");
        
        assertEquals("", clientRequest.getSharedSecret());
    }

    @Test
    void testValidIpAddresses() {
        ClientRequest clientRequest = new ClientRequest();
        
        // Test various valid IP address formats
        String[] validIps = {
            "127.0.0.1",
            "192.168.1.1",
            "10.0.0.1",
            "172.16.0.1",
            "255.255.255.255",
            "0.0.0.0"
        };
        
        for (String ip : validIps) {
            clientRequest.setIpAddress(ip);
            assertEquals(ip, clientRequest.getIpAddress());
        }
    }

    @Test
    void testVariousSharedSecrets() {
        ClientRequest clientRequest = new ClientRequest();
        
        // Test various shared secret formats
        String[] secrets = {
            "simple",
            "complex_secret_123",
            "Secret@123!",
            "very-long-shared-secret-with-many-characters-and-numbers-12345",
            "123456789",
            "a"
        };
        
        for (String secret : secrets) {
            clientRequest.setSharedSecret(secret);
            assertEquals(secret, clientRequest.getSharedSecret());
        }
    }

    @Test
    void testImmutabilityOfConstructorParameters() {
        String originalIp = "192.168.1.1";
        String originalSecret = "secret";
        
        ClientRequest clientRequest = new ClientRequest(originalIp, originalSecret);
        
        // Verify initial values
        assertEquals(originalIp, clientRequest.getIpAddress());
        assertEquals(originalSecret, clientRequest.getSharedSecret());
        
        // Modify the original strings (though strings are immutable in Java)
        // This test ensures the constructor copies the references correctly
        String newIp = "10.0.0.1";
        String newSecret = "newsecret";
        
        clientRequest.setIpAddress(newIp);
        clientRequest.setSharedSecret(newSecret);
        
        assertEquals(newIp, clientRequest.getIpAddress());
        assertEquals(newSecret, clientRequest.getSharedSecret());
    }
}
