package com.csg.airtel.aaa4j.api;

import com.csg.airtel.aaa4j.domain.failurehandling.PublishFailureHandler;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST endpoint for monitoring accounting publish metrics
 */
@Path("/api/accounting/metrics")
public class AccountingMetricsResource {

    @Inject
    PublishFailureHandler failureHandler;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PublishFailureHandler.PublishMetrics getMetrics() {
        return failureHandler.getMetrics();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public HealthStatus getHealth() {
        var metrics = failureHandler.getMetrics();
        String status = metrics.circuitState() == PublishFailureHandler.CircuitState.CLOSED ?
            "HEALTHY" : "DEGRADED";

        return new HealthStatus(
            status,
            metrics.circuitState().toString(),
            metrics.queueSize(),
            metrics.consecutiveFailures()
        );
    }

    public record HealthStatus(
        String status,
        String circuitState,
        int queuedMessages,
        int consecutiveFailures
    ) {}
}
