package com.csg.airtel.aaa4j.application.config;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
@ApplicationScoped
public class WebClientProvider {

    private final Vertx vertx;

    // Marked volatile to ensure visibility across threads after initialization
    // This follows safe publication rules for objects initialized in @PostConstruct
    private volatile WebClient webClient;
    @Inject
    public WebClientProvider(Vertx vertx) {
        this.vertx = vertx;
    }

    @PostConstruct
    void init() {
        WebClientOptions options = new WebClientOptions()
                .setMaxPoolSize(100)
                .setConnectTimeout(5000)
                .setIdleTimeout(60000)
                .setKeepAlive(true)
                .setPipelining(true)
                .setPipeliningLimit(10)
                .setHttp2MaxPoolSize(100)
                .setHttp2MultiplexingLimit(10);
        this.webClient = WebClient.create(vertx, options);
    }

    public WebClient getClient() {
        return webClient;
    }
}
