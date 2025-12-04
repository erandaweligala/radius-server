package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.domain.model.AccountingResponseEvent;
import com.csg.airtel.aaa4j.domain.model.RadiusConfig;
import io.smallrye.mutiny.Uni;
import org.aaa4j.radius.core.attribute.Attribute;
import org.aaa4j.radius.core.attribute.attributes.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ResponseHandlerTest {

    @Mock
    private RadiusClientService radiusClientService;

    @InjectMocks
    private ResponseHandler responseHandler;

    @BeforeEach
    void setup() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Inject @ConfigProperty manually
        setField("serverAddress", "127.0.0.1");
        setField("coaPort", 3799);
        setField("accountingPort", 1813);
        setField("sharedSecret", "secret123");
    }

    private void setField(String name, Object value) throws Exception {
        Field f = ResponseHandler.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(responseHandler, value);
    }


    // TEST: COA EVENT with DISCONNECT

    @Test
    void coaDisconnectInitiates() {
        AccountingResponseEvent event = mock(AccountingResponseEvent.class);

        when(event.eventId()).thenReturn("T123");
        when(event.sessionId()).thenReturn("S1");
        when(event.eventType()).thenReturn(AccountingResponseEvent.EventType.COA);
        when(event.action()).thenReturn(AccountingResponseEvent.ResponseAction.DISCONNECT);
        when(event.qosParameters()).thenReturn(Map.of("username", "john"));

        when(radiusClientService.initiate(anyList(), anyInt(), any(RadiusConfig.class)))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<Void> result = responseHandler.processAccountingResponse(event);
        result.await().indefinitely();

        verify(radiusClientService, times(1))
                .initiate(anyList(), eq(40), any(RadiusConfig.class));
    }


    // TEST: COA EVENT with FUP (should not call initiate)

    @Test
    void thandlesCoaFupWithoutRequest() {
        AccountingResponseEvent event = mock(AccountingResponseEvent.class);

        when(event.eventType()).thenReturn(AccountingResponseEvent.EventType.COA);
        when(event.action()).thenReturn(AccountingResponseEvent.ResponseAction.FUP_APPLY);

        Uni<Void> result = responseHandler.processAccountingResponse(event);
        result.await().indefinitely();

        verify(radiusClientService, never()).initiate(anyList(), anyInt(), any());
    }


    // TEST: CONTINUE event → Accounting response

    @Test
    void handlesContinueEvent() {
        AccountingResponseEvent event = mock(AccountingResponseEvent.class);

        when(event.eventId()).thenReturn("T1");
        when(event.sessionId()).thenReturn("S1");
        when(event.message()).thenReturn("OK");
        when(event.eventType()).thenReturn(AccountingResponseEvent.EventType.CONTINUE);

        when(radiusClientService.initiate(anyList(), eq(5), any()))
                .thenReturn(Uni.createFrom().voidItem());

        Uni<Void> result = responseHandler.processAccountingResponse(event);
        result.await().indefinitely();

        verify(radiusClientService, times(1))
                .initiate(argThat(list ->
                                list.stream().anyMatch(a -> a instanceof AcctSessionId)
                                        && list.stream().anyMatch(a -> a instanceof ReplyMessage)
                        ),
                        eq(5),
                        any(RadiusConfig.class)
                );
    }


    // TEST: NO_RESPONSE event → should not call radius service

    @Test
    void thandlesNoResponse() {
        AccountingResponseEvent event = mock(AccountingResponseEvent.class);
        when(event.eventType()).thenReturn(AccountingResponseEvent.EventType.NO_RESPONSE);

        Uni<Void> result = responseHandler.processAccountingResponse(event);
        result.await().indefinitely();

        verify(radiusClientService, never()).initiate(any(), anyInt(), any());
    }


    // TEST: BuildAttributes for IP + username + sessionId

    @Test
    void buildsValidAttributes() throws Exception {
        Map<String, String> qos = new HashMap<>();
        qos.put("username", "alice");
        qos.put("sessionId", "SID123");
        qos.put("nasIP", "192.168.1.1");
        qos.put("framedIP", "10.0.0.55");

        List<Attribute<?>> attrs = invokeBuildAttributes(qos);

        assertEquals(4, attrs.size());
        assertTrue(attrs.stream().anyMatch(a -> a instanceof UserName));
        assertTrue(attrs.stream().anyMatch(a -> a instanceof AcctSessionId));
        assertTrue(attrs.stream().anyMatch(a -> a instanceof NasIpAddress));
        assertTrue(attrs.stream().anyMatch(a -> a instanceof FramedIpAddress));
    }


    // TEST: Invalid IP should be skipped

    @Test
    void invalidIpIsIgnored() throws Exception {
        Map<String, String> qos = Map.of("nasIP", "999.999.999.999");

        List<Attribute<?>> attrs = invokeBuildAttributes(qos);

        assertEquals(0, attrs.size());
    }

    // Helper to call private buildAttributes()
    @SuppressWarnings("unchecked")
    private List<Attribute<?>> invokeBuildAttributes(Map<String, String> qos) throws Exception {
        var method = ResponseHandler.class.getDeclaredMethod("buildAttributes", Map.class);
        method.setAccessible(true);
        return (List<Attribute<?>>) method.invoke(responseHandler, qos);
    }


    @Test
    void onFailureInvokeBranch_isCovered() {
        // Simulate failure by mocking radiusClientService to throw exception
        AccountingResponseEvent event = mock(AccountingResponseEvent.class);
        when(event.eventType()).thenReturn(AccountingResponseEvent.EventType.CONTINUE);
        when(event.sessionId()).thenReturn("S1");
        when(event.message()).thenReturn("OK");
        when(event.eventId()).thenReturn("E1");

        when(radiusClientService.initiate(anyList(), anyInt(), any()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException("Test exception")));

        // Should not throw, logs handled internally
        responseHandler.processAccountingResponse(event)
                .subscribe().with(
                        unused -> fail("Should not succeed"),
                        ex -> assertEquals("Test exception", ex.getMessage())
                );
    }


    @Test
    void buildAttributes_handlesUnknownKeys() throws Exception {
        Map<String, String> qos = Map.of(
                "randomKey", "value"
        );

        var method = ResponseHandler.class.getDeclaredMethod("buildAttributes", Map.class);
        method.setAccessible(true);
        List<?> attrs = (List<?>) method.invoke(responseHandler, qos);

        assertTrue(attrs.isEmpty()); // unknown key ignored
    }

    @Test
    void parseIpAddress_invalidIps() throws Exception {
        var method = ResponseHandler.class.getDeclaredMethod("parseIpAddress", String.class);
        method.setAccessible(true);

        // IPv6 address
        Optional<?> result = (Optional<?>) method.invoke(responseHandler, "abcd::1234");
        assertTrue(result.isEmpty());

        // Invalid IPv4
        result = (Optional<?>) method.invoke(responseHandler, "999.999.999.999");
        assertTrue(result.isEmpty());
    }







}
