package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import com.csg.airtel.aaa4j.domain.producer.RadiusAccountingProducer;
import org.aaa4j.radius.core.attribute.EnumData;
import org.aaa4j.radius.core.attribute.Ipv4AddrData;
import org.aaa4j.radius.core.attribute.IntegerData;
import org.aaa4j.radius.core.attribute.TextData;
import org.aaa4j.radius.core.attribute.attributes.*;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.packets.AccountingRequest;
import org.aaa4j.radius.core.packet.packets.AccountingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RadiusAccountingHandlerTest {

    // Verifies RadiusAccountingHandler behavior for different accounting flows and error paths

    @Mock
    private RadiusAccountingProducer accountingProducer;

    @InjectMocks
    private RadiusAccountingHandler handler;

    private InetAddress clientAddress;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        handler = new RadiusAccountingHandler(accountingProducer);
        handler.sharedSecret = "sharedsecret";
        clientAddress = InetAddress.getByName("127.0.0.1");
        when(accountingProducer.produceAccountingEvent(any(AccountingRequestDto.class)))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void handleClient_ReturnSharedSecretBytes() {
        // Expect the configured shared secret to be returned for client-specific secret negotiation
        byte[] result = handler.handleClient(clientAddress);
        assertArrayEquals("sharedsecret".getBytes(), result);
    }

    @Test
    void handleNonAccountingRequest_returnsNull() {
        // Non-accounting packets should be ignored by the accounting handler
        Packet nonAccounting = new Packet(0, List.of(new MessageAuthenticator()));
        Packet result = handler.handlePacket(clientAddress, nonAccounting);
        assertNull(result);
    }

    @Test
    void nullOnMissingStatusType() {
        // Missing Acct-Status-Type => handler cannot determine action; should not produce any event
        AccountingRequest request = new AccountingRequest(List.of(
                new MessageAuthenticator(),
                new UserName(new TextData("user")),
                new AcctSessionId(new TextData("sess1"))
        ));

        Packet result = handler.handlePacket(clientAddress, request);

        assertNull(result);
        verify(accountingProducer, never()).produceAccountingEvent(any());
    }

    @Test
    void producesStartEventAndReturnsResponse() {
        // Acct-Status-Type = 1 (START) should emit START event and return AccountingResponse with ACK message
        AccountingRequest request = new AccountingRequest(List.of(
                new MessageAuthenticator(),
                new UserName(new TextData("user1")),
                new AcctSessionId(new TextData("session123")),
                new AcctStatusType(new EnumData(1)),
                new AcctInputOctets(new IntegerData(1000)),
                new AcctOutputOctets(new IntegerData(2000)),
                new AcctSessionTime(new IntegerData(300)),
                new NasIpAddress(new Ipv4AddrData((Inet4Address) clientAddress))
        ));

        Packet response = handler.handlePacket(clientAddress, request);

        assertNotNull(response);
        assertInstanceOf(AccountingResponse.class, response);
        verify(accountingProducer, times(1)).produceAccountingEvent(argThat(dto ->
                dto.sessionId().equals("session123") &&
                        dto.username().equals("user1") &&
                        dto.actionType() == AccountingRequestDto.ActionType.START
        ));
    }

    @Test
    void producesStopEventAndReturnsResponse() {
        // Acct-Status-Type = 2 (STOP) should emit STOP event with provided counters
        AccountingRequest request = new AccountingRequest(List.of(
                new MessageAuthenticator(),
                new UserName(new TextData("user2")),
                new AcctSessionId(new TextData("sess-stop")),
                new AcctStatusType(new EnumData(2)),
                new AcctInputOctets(new IntegerData(10)),
                new AcctOutputOctets(new IntegerData(20))
        ));

        Packet response = handler.handlePacket(clientAddress, request);

        assertNotNull(response);
        assertInstanceOf(AccountingResponse.class, response);
        verify(accountingProducer, times(1)).produceAccountingEvent(argThat(dto ->
                dto.sessionId().equals("sess-stop") &&
                        dto.username().equals("user2") &&
                        dto.actionType() == AccountingRequestDto.ActionType.STOP
        ));
    }

    @Test
    void producesInterimEventAndReturnsResponse() {
        // Acct-Status-Type = 3 (INTERIM-UPDATE) should emit INTERIM_UPDATE event
        AccountingRequest request = new AccountingRequest(List.of(
                new MessageAuthenticator(),
                new UserName(new TextData("user3")),
                new AcctSessionId(new TextData("sess-interim")),
                new AcctStatusType(new EnumData(3)),
                new AcctInputOctets(new IntegerData(111)),
                new AcctOutputOctets(new IntegerData(222))
        ));

        Packet response = handler.handlePacket(clientAddress, request);

        assertNotNull(response);
        assertInstanceOf(AccountingResponse.class, response);
        verify(accountingProducer, times(1)).produceAccountingEvent(argThat(dto ->
                dto.sessionId().equals("sess-interim") &&
                        dto.username().equals("user3") &&
                        dto.actionType() == AccountingRequestDto.ActionType.INTERIM_UPDATE
        ));
    }

    @Test
    void producerThrows_returnsNull() {
        // If downstream producer fails, handler should catch and return null (indicating failure)
        when(accountingProducer.produceAccountingEvent(any(AccountingRequestDto.class)))
                .thenThrow(new RuntimeException("producer error"));

        AccountingRequest request = new AccountingRequest(List.of(
                new MessageAuthenticator(),
                new UserName(new TextData("user4")),
                new AcctSessionId(new TextData("sess-err")),
                new AcctStatusType(new EnumData(1))
        ));

        Packet response = handler.handlePacket(clientAddress, request);

        assertNull(response);
    }

    @Test
    void rateLimitEnforced_dropsRequestsAbove500TPS() throws Exception {
        // Test that requests exceeding 500 TPS are rate-limited and dropped
        AccountingRequest request = new AccountingRequest(List.of(
                new MessageAuthenticator(),
                new UserName(new TextData("ratelimituser")),
                new AcctSessionId(new TextData("sess-ratelimit")),
                new AcctStatusType(new EnumData(1)),
                new NasIpAddress(new Ipv4AddrData((Inet4Address) clientAddress))
        ));

        int successfulRequests = 0;
        int droppedRequests = 0;
        int totalRequests = 600; // Send more than the 500 TPS limit

        // Send requests rapidly
        for (int i = 0; i < totalRequests; i++) {
            Packet response = handler.handlePacket(clientAddress, request);
            if (response != null) {
                successfulRequests++;
            } else {
                droppedRequests++;
            }
        }

        // Verify that approximately 500 requests were processed and 100 were dropped
        assertTrue(successfulRequests <= 500, "Should not process more than 500 requests");
        assertTrue(droppedRequests >= 100, "Should drop excess requests beyond 500 TPS limit");
        assertEquals(totalRequests, successfulRequests + droppedRequests, "All requests should be accounted for");
    }

    @Test
    void rateLimitAllowsRequestsWithinLimit() throws Exception {
        // Test that requests within the 500 TPS limit are processed normally
        AccountingRequest request = new AccountingRequest(List.of(
                new MessageAuthenticator(),
                new UserName(new TextData("normaluser")),
                new AcctSessionId(new TextData("sess-normal")),
                new AcctStatusType(new EnumData(1)),
                new NasIpAddress(new Ipv4AddrData((Inet4Address) clientAddress))
        ));

        int successfulRequests = 0;
        int requestsToSend = 400; // Well within the 500 TPS limit

        for (int i = 0; i < requestsToSend; i++) {
            Packet response = handler.handlePacket(clientAddress, request);
            if (response != null) {
                successfulRequests++;
            }
        }

        // All requests should be processed successfully
        assertEquals(requestsToSend, successfulRequests, "All requests within limit should be processed");
        verify(accountingProducer, times(requestsToSend)).produceAccountingEvent(any(AccountingRequestDto.class));
    }
}


