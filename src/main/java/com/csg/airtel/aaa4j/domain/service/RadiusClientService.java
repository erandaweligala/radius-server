package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.domain.model.RadiusConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import org.aaa4j.radius.client.RadiusClient;
import org.aaa4j.radius.client.RadiusClientException;
import org.aaa4j.radius.client.clients.UdpRadiusClient;
import org.aaa4j.radius.core.attribute.Attribute;
import org.aaa4j.radius.core.packet.Packet;
import org.jboss.logging.Logger;

import java.net.InetSocketAddress;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

@ApplicationScoped
public class RadiusClientService {
    private static final Logger logger = Logger.getLogger(RadiusClientService.class);

    private static final int COA_ACK = 44;
    private static final int COA_NAK = 45;

    /**
     * Sends a COA (Change of Authorization) request to a RADIUS server reactively
     *
     * @param attributes The list of RADIUS attributes
     * @param code The RADIUS packet code
     * @param radiusConfig The RADIUS server configuration
     * @return Uni<Void> completing when the operation is done
     */
    public Uni<Void> initiate(List<Attribute<?>> attributes, int code, RadiusConfig radiusConfig) {
        logger.infof("Initiating COA request to RADIUS server at %s:%d",
                radiusConfig.serverAddress(), radiusConfig.port());

        return Uni.createFrom().item(() -> createRadiusClient(radiusConfig))
                .chain(radiusClient -> sendCoaRequest(radiusClient, attributes, code))
                .chain(this::processResponse)
                .onFailure(RadiusClientException.class)
                .invoke(e -> logger.error("RADIUS client error while sending COA request", e))
                .onFailure()
                .invoke(e -> logger.error("Unexpected error during COA request", e))
                .replaceWithVoid();
    }

    /**
     * Creates a RADIUS client with the given configuration
     */
    private RadiusClient createRadiusClient(RadiusConfig radiusConfig) {
        return UdpRadiusClient.newBuilder()
                .secret(radiusConfig.sharedSecret().getBytes(UTF_8))
                .address(new InetSocketAddress(radiusConfig.serverAddress(), radiusConfig.port()))
                .build();
    }

    /**
     * Sends the COA request reactively
     */
    private Uni<Packet> sendCoaRequest(RadiusClient radiusClient, List<Attribute<?>> attributes, int code) {
        return Uni.createFrom().item(() -> {
                    logger.infof("Processing for code: %s", code);
                    Packet coaRequest = new Packet(code, attributes);
                    logger.infof("Sending packet  with %s attributes", attributes.size());
                    return coaRequest;
                })
                .chain(coaRequest ->
                        Uni.createFrom().item(() -> {
                            try {
                                return radiusClient.send(coaRequest);
                            } catch (RadiusClientException e) {
                                logger.error("RADIUS client error while sending packet", e);
                            }
                            return null;
                        })
                )
                .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()); // Execute blocking I/O on worker thread
    }

    /**
     * Process the RADIUS response packet reactively
     */
    private Uni<Void> processResponse(Packet responsePacket) {
        return Uni.createFrom().voidItem()
                .invoke(() -> {
                    if (responsePacket == null) {
                        logger.warn("Received null response packet");
                        return;
                    }

                    int responseCode = responsePacket.getCode();
                    logger.infof("Received RADIUS response with code: %d", responseCode);

                    switch (responseCode) {
                        case COA_ACK ->
                                logger.info("COA request acknowledged successfully");
                        case COA_NAK ->
                                logger.warn("COA request rejected by RADIUS server");
                        default ->
                                logger.warnf("Unexpected response code: %d", responseCode);
                    }
                });
    }
}