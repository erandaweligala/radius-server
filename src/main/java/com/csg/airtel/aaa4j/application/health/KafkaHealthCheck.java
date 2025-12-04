package com.csg.airtel.aaa4j.application.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;

import java.time.Duration;
import java.util.Properties;

/**
 * Health check for Kafka broker connectivity
 */
@Readiness
@ApplicationScoped
public class KafkaHealthCheck implements HealthCheck {

    @ConfigProperty(name = "mp.messaging.outgoing.accounting-events.bootstrap.servers")
    String bootstrapServers;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder responseBuilder = HealthCheckResponse.named("Kafka Broker");

        try {
            Properties props = new Properties();
            props.put("bootstrap.servers", bootstrapServers);
            props.put("connections.max.idle.ms", "10000");
            props.put("request.timeout.ms", "5000");

            // Try to create admin client and list topics (quick connectivity check)
            try (org.apache.kafka.clients.admin.AdminClient adminClient =
                         org.apache.kafka.clients.admin.AdminClient.create(props)) {

                // List topics with timeout
                var result = adminClient.listTopics();
                result.names().get(3, java.util.concurrent.TimeUnit.SECONDS);

                return responseBuilder
                        .up()
                        .withData("bootstrap.servers", bootstrapServers)
                        .build();
            }
        } catch (Exception e) {
            return responseBuilder
                    .down()
                    .withData("bootstrap.servers", bootstrapServers)
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
