package com.csg.airtel.aaa4j.application.health;

import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.time.Duration;

/**
 * Health check for external authentication service
 */
@Readiness
@ApplicationScoped
public class AuthServiceHealthCheck implements HealthCheck {

    @Inject
    com.csg.airtel.aaa4j.application.config.WebClientProvider webClientProvider;

    @ConfigProperty(name = "auth.service.url")
    String authServiceUrl;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Authentication Service");

        try {
            // Extract base URL (remove path for health check)
            String baseUrl = authServiceUrl.substring(0, authServiceUrl.indexOf("/api"));
            String healthUrl = baseUrl + "/q/health";

            WebClient client = webClientProvider.getMutinyClient();

            // Try to reach the service with timeout
            var response = client.getAbs(healthUrl)
                    .send()
                    .ifNoItem().after(Duration.ofSeconds(3))
                    .fail()
                    .await().atMost(Duration.ofSeconds(4));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return responseBuilder
                        .up()
                        .withData("url", authServiceUrl)
                        .withData("status", response.statusCode())
                        .build();
            } else {
                return responseBuilder
                        .down()
                        .withData("url", authServiceUrl)
                        .withData("status", response.statusCode())
                        .build();
            }
        } catch (Exception e) {
            return responseBuilder
                    .down()
                    .withData("url", authServiceUrl)
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
