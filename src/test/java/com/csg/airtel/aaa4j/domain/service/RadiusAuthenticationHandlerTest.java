package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.domain.model.UserDetails;
import com.csg.airtel.aaa4j.external.client.AuthManagementServiceClient;
import org.aaa4j.radius.core.attribute.StringData;
import org.aaa4j.radius.core.attribute.attributes.*;
import org.aaa4j.radius.core.attribute.TextData;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.packets.AccessReject;
import org.aaa4j.radius.core.packet.packets.AccessRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.InetAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RadiusAuthenticationHandlerTest {

    @Mock
    private AuthManagementServiceClient authManagementServiceClient;

    @InjectMocks
    private RadiusAuthenticationHandler handler;

    private InetAddress clientAddress;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        handler = new RadiusAuthenticationHandler(authManagementServiceClient);
        clientAddress = InetAddress.getByName("127.0.0.1");
        handler.sharedSecret = "test-secret";
    }

    @Test
    void handleClient_shouldReturnSharedSecretBytes() {
        byte[] result = handler.handleClient(clientAddress);
        assertArrayEquals("test-secret".getBytes(), result);
    }

    @Test
    void handlePacket_withNonAccessRequest_shouldReturnNull() {
        Packet nonAccessRequest = mock(Packet.class);
        
        Packet result = handler.handlePacket(clientAddress, nonAccessRequest);
        
        assertNull(result);
    }

    @Test
    void handlePacket_withMissingMessageAuthenticator_shouldReturnAccessReject() {
        AccessRequest request = new AccessRequest(List.of(
            new UserName(new TextData("testuser"))
        ));
        
        Packet result = handler.handlePacket(clientAddress, request);
        
        assertInstanceOf(AccessReject.class, result);
    }

    @Test
    void handlePacket_withMissingUserName_shouldReturnAccessReject() {
        AccessRequest request = new AccessRequest(List.of(
            new MessageAuthenticator()
        ));
        
        Packet result = handler.handlePacket(clientAddress, request);
        
        assertInstanceOf(AccessReject.class, result);
    }


    @Test
    void handlePacket_withInactiveUser_shouldReturnAccessReject() throws InterruptedException {
        UserDetails userDetails = new UserDetails();
        userDetails.setIsActive(false);
        userDetails.setIsAuthorized(true);
        
        when(authManagementServiceClient.authenticate(eq("testuser"), any(), any(), any(), any()))
            .thenReturn(userDetails);
        
        AccessRequest request = new AccessRequest(List.of(
            new MessageAuthenticator(),
            new UserName(new TextData("testuser")),
                new UserPassword(new StringData("pw".getBytes()))
        ));
        
        Packet result = handler.handlePacket(clientAddress, request);
        
        assertInstanceOf(AccessReject.class, result);
    }

    @Test
    void handlePacket_withUnauthorizedUser_shouldReturnAccessReject() throws InterruptedException {
        UserDetails userDetails = new UserDetails();
        userDetails.setIsActive(true);
        userDetails.setIsAuthorized(false);
        
        when(authManagementServiceClient.authenticate(eq("testuser"), any(), any(), any(), any()))
            .thenReturn(userDetails);
        
        AccessRequest request = new AccessRequest(List.of(
            new MessageAuthenticator(),
            new UserName(new TextData("testuser")),
                new UserPassword(new StringData("pw".getBytes()))
        ));
        
        Packet result = handler.handlePacket(clientAddress, request);
        
        assertInstanceOf(AccessReject.class, result);
    }

    @Test
    void handlePacket_withInterruptedException_shouldReturnAccessReject() throws InterruptedException {
        when(authManagementServiceClient.authenticate(any(), any(), any(), any(), any()))
            .thenThrow(new InterruptedException("Test interruption"));
        
        AccessRequest request = new AccessRequest(List.of(
            new MessageAuthenticator(),
            new UserName(new TextData("testuser")),
                new UserPassword(new StringData("pw".getBytes()))
        ));
        
        Packet result = handler.handlePacket(clientAddress, request);
        
        assertInstanceOf(AccessReject.class, result);
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    void handlePacket_withException_shouldReturnAccessReject() throws InterruptedException {
        when(authManagementServiceClient.authenticate(any(), any(), any(), any(), any()))
            .thenThrow(new RuntimeException("Test exception"));
        
        AccessRequest request = new AccessRequest(List.of(
            new MessageAuthenticator(),
            new UserName(new TextData("testuser")),
                new UserPassword(new StringData("pw".getBytes()))
        ));
        
        Packet result = handler.handlePacket(clientAddress, request);
        
        assertInstanceOf(AccessReject.class, result);
    }
}
