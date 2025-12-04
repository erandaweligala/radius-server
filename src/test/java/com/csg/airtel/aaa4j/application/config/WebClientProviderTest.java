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

    private WebClientProvider webClientProvider;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webClientProvider = new WebClientProvider(vertx);
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
