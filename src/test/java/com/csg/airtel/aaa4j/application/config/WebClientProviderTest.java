package com.csg.airtel.aaa4j.application.config;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WebClientProviderTest {

    @Mock
    private Vertx vertx;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClientConfig webClientConfig;

    private WebClientProvider webClientProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup default config values
        when(webClientConfig.maxPoolSize()).thenReturn(250);
        when(webClientConfig.connectTimeout()).thenReturn(5000);
        when(webClientConfig.idleTimeout()).thenReturn(60000);
        when(webClientConfig.keepAlive()).thenReturn(true);
        when(webClientConfig.pipelining()).thenReturn(true);
        when(webClientConfig.pipeliningLimit()).thenReturn(10);
        when(webClientConfig.http2MaxPoolSize()).thenReturn(250);
        when(webClientConfig.http2MultiplexingLimit()).thenReturn(100);
        when(webClientConfig.http2KeepAliveTimeout()).thenReturn(60);

        webClientProvider = new WebClientProvider(vertx, webClientConfig);
    }

    @Test
    void testConstructor() {
        assertNotNull(webClientProvider);
    }

    @Test
    void testInit() {
        try (MockedStatic<WebClient> webClientMock = mockStatic(WebClient.class)) {
            webClientMock.when(() -> WebClient.create(eq(vertx), any()))
                    .thenReturn(webClient);

            webClientProvider.init();

            WebClient result = webClientProvider.getClient();
            assertEquals(webClient, result);

            webClientMock.verify(() -> WebClient.create(eq(vertx), any()), times(1));
        }
    }

    @Test
    void testGetClient_BeforeInit() {
        WebClient result = webClientProvider.getClient();
        assertNull(result);
    }

    @Test
    void testGetClient_AfterInit() {
        try (MockedStatic<WebClient> webClientMock = mockStatic(WebClient.class)) {
            webClientMock.when(() -> WebClient.create(eq(vertx), any()))
                    .thenReturn(webClient);

            webClientProvider.init();
            WebClient result = webClientProvider.getClient();

            assertNotNull(result);
            assertEquals(webClient, result);
        }
    }
}
