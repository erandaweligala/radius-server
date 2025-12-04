package com.csg.airtel.aaa4j.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

public record AccountingResponseEvent(
        @JsonProperty("eventId") String eventId,
        @JsonProperty("eventType") EventType eventType,
        @JsonProperty("eventTime") LocalDateTime eventTime,
        @JsonProperty("sessionId") String sessionId,
        @JsonProperty("action") ResponseAction action,
        @JsonProperty("message") String message,
        @JsonProperty("totalQuotaBalance") Long totalQuotaBalance,
        @JsonProperty("fupProfileId") Map<String,String> qosParameters
) {
    public enum ResponseAction {
        DISCONNECT,
        FUP_APPLY,
        INTERNAL_ERROR,
        IGNORE_PROCESSING,
        SUCCESS

    }
    public enum EventType {
       COA, CONTINUE,NO_RESPONSE
    }
}