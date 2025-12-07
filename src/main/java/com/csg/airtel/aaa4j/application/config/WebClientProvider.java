package com.csg.airtel.aaa4j.application.config;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
@ApplicationScoped
public class WebClientProvider {

    //todo this WebClientOptions varible values get set yml file
    private final Vertx vertx;
    private WebClient webClient;
    @Inject
    public WebClientProvider(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Initialize WebClient with connection pool settings optimized for 1000 TPS.
     *
     * Pool sizing rationale:
     * - HTTP/1.1 pool: 250 connections (fallback for non-HTTP/2 servers)
     * - HTTP/2 pool: 250 connections with 100 streams each = 25,000 concurrent requests
     * - With HTTP/2 multiplexing, fewer connections needed for high throughput
     */
    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                // HTTP/1.1 connection pool (fallback)
                .setMaxPoolSize(250)
                // Connection timeout (5 seconds)
                .setConnectTimeout(5000)
                // Idle timeout (60 seconds) - keep connections warm
                .setIdleTimeout(60000)
                // Keep connections alive for reuse
                .setKeepAlive(true)
                // Enable HTTP pipelining for HTTP/1.1
                .setPipelining(true)
                .setPipeliningLimit(10)
                // HTTP/2 connection pool (primary for high throughput)
                .setHttp2MaxPoolSize(250)
                // HTTP/2 streams per connection (multiplexing)
                .setHttp2MultiplexingLimit(100)
                // Connection keep-alive interval
                .setHttp2KeepAliveTimeout(60);

        this.webClient = WebClient.create(vertx, options);
    }

    public WebClient getClient() {
        return webClient;
    }
}
