package com.csg.airtel.aaa4j.external.client;

import com.csg.airtel.aaa4j.application.config.WebClientProvider;
import com.csg.airtel.aaa4j.domain.model.UserDetails;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthManagementServiceClientTest {

    @Mock
    private WebClientProvider webClientProvider;

    @Mock
    private WebClient webClient;

    @Mock
    private HttpRequest<Buffer> httpRequest;

    @Mock
    private HttpResponse<Buffer> httpResponse;

    @InjectMocks
    private AuthManagementServiceClient authClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webClientProvider.getClient()).thenReturn(webClient);

        // manually inject config property
        authClient.authServiceUrl = "http://localhost:8083/api/users/authenticate";
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }
    @Test
    void testAuthenticate_failureResponse_shouldReturnFallbackUser(){
        // Given
        String username = "user2";
        when(webClient.postAbs(anyString())).thenReturn(httpRequest);
        when(httpRequest.putHeader(anyString(), anyString())).thenReturn(httpRequest);

        doAnswer(invocation -> {
            Handler<AsyncResult<HttpResponse<Buffer>>> handler = invocation.getArgument(1);
            AsyncResult<HttpResponse<Buffer>> asyncResult = mock(AsyncResult.class);
            when(asyncResult.succeeded()).thenReturn(false);
            when(asyncResult.cause()).thenReturn(new RuntimeException("Connection failed"));
            handler.handle(asyncResult);
            return null;
        }).when(httpRequest).sendJsonObject(any(JsonObject.class), any());

        // When
        UserDetails result;
        try {
            result = authClient.authenticate(username, "pass", null, null, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // restore interrupt flag
            fail("Test interrupted unexpectedly: " + e.getMessage());
            return;
        }
        // Then
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(httpRequest, atLeastOnce()).sendJsonObject(any(), any());
    }

}
