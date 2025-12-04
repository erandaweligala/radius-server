package com.csg.airtel.aaa4j.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TraceIdGeneratorTest {

    @Test
    void testGenerateTraceId_NotNull() {
        String traceId = TraceIdGenerator.generateTraceId();
        assertNotNull(traceId);
    }

    @Test
    void testGenerateTraceId_NotEmpty() {
        String traceId = TraceIdGenerator.generateTraceId();
        assertFalse(traceId.isEmpty());
    }

    @Test
    void testGenerateTraceId_ContainsDash() {
        String traceId = TraceIdGenerator.generateTraceId();
        assertTrue(traceId.contains("-"));
    }

    @Test
    void testGenerateTraceId_HasCorrectFormat() {
        String traceId = TraceIdGenerator.generateTraceId();
        
        // Should contain exactly one dash
        long dashCount = traceId.chars().filter(ch -> ch == '-').count();
        assertEquals(1, dashCount);
        
        // Split by dash and check parts
        String[] parts = traceId.split("-");
        assertEquals(2, parts.length);
        
        // First part should be 8 characters (UUID part)
        assertEquals(8, parts[0].length());
        
        // Second part should be 17 characters (timestamp part)
        assertEquals(17, parts[1].length());
        
        // Both parts should contain only valid characters
        assertTrue(parts[0].matches("[a-f0-9]+"));
        assertTrue(parts[1].matches("[0-9]+"));
    }

    @Test
    void testGenerateTraceId_Uniqueness() {
        Set<String> traceIds = new HashSet<>();
        
        // Generate multiple trace IDs and ensure they're unique
        for (int i = 0; i < 1000; i++) {
            String traceId = TraceIdGenerator.generateTraceId();
            assertTrue(traceIds.add(traceId), "Duplicate trace ID generated: " + traceId);
        }
    }

    @Test
    void testGenerateTraceId_ConsistentLength() {
        String traceId1 = TraceIdGenerator.generateTraceId();
        String traceId2 = TraceIdGenerator.generateTraceId();
        
        assertEquals(traceId1.length(), traceId2.length());
        
        // Expected length: 8 (UUID) + 1 (dash) + 17 (timestamp) = 26
        assertEquals(26, traceId1.length());
    }

    @Test
    void testGenerateTraceId_MultipleCallsReturnDifferentValues() {
        String traceId1 = TraceIdGenerator.generateTraceId();
        String traceId2 = TraceIdGenerator.generateTraceId();
        
        assertNotEquals(traceId1, traceId2);
    }
}
