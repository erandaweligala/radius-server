package com.csg.airtel.aaa4j.application.health;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.time.Duration;

/**
 * Health check for Redis cache
 */
@Readiness
@ApplicationScoped
public class RedisHealthCheck implements HealthCheck {

    @Inject
    ReactiveRedisDataSource redisDataSource;

    @ConfigProperty(name = "auth.cache.enabled", defaultValue = "true")
    boolean cacheEnabled;

    @ConfigProperty(name = "quarkus.redis.hosts")
    String redisHosts;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Redis Cache");

        if (!cacheEnabled) {
            return responseBuilder
                    .up()
                    .withData("status", "disabled")
                    .build();
        }

        try {
            // Ping Redis with timeout
            String pong = redisDataSource.execute("PING")
                    .await().atMost(Duration.ofSeconds(2));

            if ("PONG".equalsIgnoreCase(pong)) {
                return responseBuilder
                        .up()
                        .withData("hosts", redisHosts)
                        .withData("response", pong)
                        .build();
            } else {
                return responseBuilder
                        .down()
                        .withData("hosts", redisHosts)
                        .withData("error", "Unexpected response: " + pong)
                        .build();
            }
        } catch (Exception e) {
            return responseBuilder
                    .down()
                    .withData("hosts", redisHosts)
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
