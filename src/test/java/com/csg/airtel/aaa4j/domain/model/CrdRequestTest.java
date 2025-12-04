package com.csg.airtel.aaa4j.domain.model;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class CrdRequestTest {

    @Test
    void testDefaultConstructor() {
        CrdRequest crdRequest = new CrdRequest();
        
        assertNull(crdRequest.getUsername());
        assertNull(crdRequest.getSessionId());
        assertNull(crdRequest.getNasIp());
        assertEquals(0, crdRequest.getInputOctets());
        assertEquals(0, crdRequest.getOutputOctets());
        assertEquals(0, crdRequest.getSessionTime());
        assertNull(crdRequest.getStartTime());
        assertNull(crdRequest.getStopTime());
    }

    @Test
    void testSettersAndGetters() {
        CrdRequest crdRequest = new CrdRequest();
        Date startTime = new Date();
        Date stopTime = new Date(startTime.getTime() + 60000); // 1 minute later
        
        crdRequest.setUsername("testuser");
        crdRequest.setSessionId("session123");
        crdRequest.setNasIp("192.168.1.1");
        crdRequest.setInputOctets(1000L);
        crdRequest.setOutputOctets(2000L);
        crdRequest.setSessionTime(300L);
        crdRequest.setStartTime(startTime);
        crdRequest.setStopTime(stopTime);
        
        assertEquals("testuser", crdRequest.getUsername());
        assertEquals("session123", crdRequest.getSessionId());
        assertEquals("192.168.1.1", crdRequest.getNasIp());
        assertEquals(1000L, crdRequest.getInputOctets());
        assertEquals(2000L, crdRequest.getOutputOctets());
        assertEquals(300L, crdRequest.getSessionTime());
        assertEquals(startTime, crdRequest.getStartTime());
        assertEquals(stopTime, crdRequest.getStopTime());
    }

    @Test
    void testLongValues() {
        CrdRequest crdRequest = new CrdRequest();
        
        // Test with large values
        long largeInput = Long.MAX_VALUE;
        long largeOutput = Long.MAX_VALUE - 1;
        long largeSessionTime = 86400L; // 24 hours in seconds
        
        crdRequest.setInputOctets(largeInput);
        crdRequest.setOutputOctets(largeOutput);
        crdRequest.setSessionTime(largeSessionTime);
        
        assertEquals(largeInput, crdRequest.getInputOctets());
        assertEquals(largeOutput, crdRequest.getOutputOctets());
        assertEquals(largeSessionTime, crdRequest.getSessionTime());
    }

    @Test
    void testNegativeValues() {
        CrdRequest crdRequest = new CrdRequest();
        
        // Test with negative values (though not realistic for accounting)
        crdRequest.setInputOctets(-100L);
        crdRequest.setOutputOctets(-200L);
        crdRequest.setSessionTime(-300L);
        
        assertEquals(-100L, crdRequest.getInputOctets());
        assertEquals(-200L, crdRequest.getOutputOctets());
        assertEquals(-300L, crdRequest.getSessionTime());
    }

    @Test
    void testZeroValues() {
        CrdRequest crdRequest = new CrdRequest();
        
        crdRequest.setInputOctets(0L);
        crdRequest.setOutputOctets(0L);
        crdRequest.setSessionTime(0L);
        
        assertEquals(0L, crdRequest.getInputOctets());
        assertEquals(0L, crdRequest.getOutputOctets());
        assertEquals(0L, crdRequest.getSessionTime());
    }

    @Test
    void testNullStringValues() {
        CrdRequest crdRequest = new CrdRequest();
        
        crdRequest.setUsername(null);
        crdRequest.setSessionId(null);
        crdRequest.setNasIp(null);
        
        assertNull(crdRequest.getUsername());
        assertNull(crdRequest.getSessionId());
        assertNull(crdRequest.getNasIp());
    }

    @Test
    void testEmptyStringValues() {
        CrdRequest crdRequest = new CrdRequest();
        
        crdRequest.setUsername("");
        crdRequest.setSessionId("");
        crdRequest.setNasIp("");
        
        assertEquals("", crdRequest.getUsername());
        assertEquals("", crdRequest.getSessionId());
        assertEquals("", crdRequest.getNasIp());
    }

    @Test
    void testDateValues() {
        CrdRequest crdRequest = new CrdRequest();
        Date now = new Date();
        Date future = new Date(now.getTime() + 3600000); // 1 hour later
        
        crdRequest.setStartTime(now);
        crdRequest.setStopTime(future);
        
        assertEquals(now, crdRequest.getStartTime());
        assertEquals(future, crdRequest.getStopTime());
        assertTrue(crdRequest.getStopTime().after(crdRequest.getStartTime()));
    }

    @Test
    void testNullDateValues() {
        CrdRequest crdRequest = new CrdRequest();
        
        crdRequest.setStartTime(null);
        crdRequest.setStopTime(null);
        
        assertNull(crdRequest.getStartTime());
        assertNull(crdRequest.getStopTime());
    }

    @Test
    void testToString() {
        CrdRequest crdRequest = new CrdRequest();
        crdRequest.setUsername("testuser");
        crdRequest.setSessionId("session123");
        crdRequest.setNasIp("192.168.1.1");
        crdRequest.setInputOctets(1000L);
        crdRequest.setOutputOctets(2000L);
        crdRequest.setSessionTime(300L);
        crdRequest.setStartTime(new Date());
        
        String toString = crdRequest.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("CrdRequest"));
        assertTrue(toString.contains("testuser"));
        assertTrue(toString.contains("session123"));
        assertTrue(toString.contains("192.168.1.1"));
        assertTrue(toString.contains("1000"));
        assertTrue(toString.contains("2000"));
        assertTrue(toString.contains("300"));
    }

    @Test
    void testToString_WithNullValues() {
        CrdRequest crdRequest = new CrdRequest();
        
        String toString = crdRequest.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("CrdRequest"));
        assertTrue(toString.contains("null"));
    }
}
