package com.csg.airtel.aaa4j.application.config;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx as MutinyVertx;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class WebClientProvider {

    private final Vertx vertx;
    private io.vertx.ext.web.client.WebClient webClient;
    private io.vertx.mutiny.ext.web.client.WebClient mutinyWebClient;

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

        // Create both standard and Mutiny WebClients
        this.webClient = io.vertx.ext.web.client.WebClient.create(vertx, options);

        // Create Mutiny WebClient for reactive operations
        MutinyVertx mutinyVertx = new MutinyVertx(vertx);
        this.mutinyWebClient = io.vertx.mutiny.ext.web.client.WebClient.create(mutinyVertx, options);
    }

    /**
     * Get standard WebClient (deprecated - use getMutinyClient for reactive operations)
     */
    public io.vertx.ext.web.client.WebClient getClient() {
        return webClient;
    }

    /**
     * Get Mutiny WebClient for reactive operations
     */
    public io.vertx.mutiny.ext.web.client.WebClient getMutinyClient() {
        return mutinyWebClient;
    }
}
