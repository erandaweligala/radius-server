package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.common.constant.AuthServiceConstants;
import com.csg.airtel.aaa4j.common.util.TraceIdGenerator;
import com.csg.airtel.aaa4j.domain.dictionary.NokiaDictionary;
import com.csg.airtel.aaa4j.domain.model.UserDetails;
import com.csg.airtel.aaa4j.external.client.AuthManagementServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import org.aaa4j.radius.core.attribute.Attribute;
import org.aaa4j.radius.core.attribute.IntegerData;
import org.aaa4j.radius.core.attribute.TextData;
import org.aaa4j.radius.core.attribute.attributes.*;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.packets.AccessAccept;
import org.aaa4j.radius.core.packet.packets.AccessReject;
import org.aaa4j.radius.core.packet.packets.AccessRequest;
import org.aaa4j.radius.server.RadiusServer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.slf4j.MDC;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import java.util.*;

@ApplicationScoped
public class RadiusAuthenticationHandler implements RadiusServer.Handler {
    private static final Logger logger = Logger.getLogger(RadiusAuthenticationHandler.class);

    @ConfigProperty(name = "radius.shared-secret")
    String sharedSecret;

    final AuthManagementServiceClient authManagementServiceClient;

    public RadiusAuthenticationHandler(AuthManagementServiceClient authManagementServiceClient) {
        this.authManagementServiceClient = authManagementServiceClient;
    }

    @Override
    public byte[] handleClient(InetAddress clientAddress) {
        return sharedSecret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public Packet handlePacket(InetAddress clientAddress, Packet requestPacket) {
        String traceId = TraceIdGenerator.generateTraceId();
        MDC.put(AuthServiceConstants.TRACE_ID, traceId);

        Instant startTime = Instant.now();
        logger.infof("[%s] [RADIUS_TIMING] [AUTHENTICATION] Request started at: %s", traceId, startTime);

        try {
            logger.infof("[%s] handlePacket() triggered — processing RADIUS request", traceId);
            logPacketReceived(traceId, clientAddress, requestPacket);

            if (!(requestPacket instanceof AccessRequest)) {
                logger.warnf("[%s] Unsupported packet type received: %s", traceId, requestPacket.getClass().getSimpleName());
                return null;
            }

            return handleAccessRequest(traceId, (AccessRequest) requestPacket);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            logger.errorf("[%s] Packet processing interrupted: %s", traceId, ie.getMessage(), ie);
            return buildAccessReject("Request interrupted");

        } catch (Exception e) {
            logger.errorf("[%s] Error while processing packet: %s", traceId, e.getMessage(), e);
            return buildAccessReject("Internal server error occurred. Please try again later.");
        } finally {
            Instant endTime = Instant.now();
            long durationMs = Duration.between(startTime, endTime).toMillis();
            logger.infof("[%s] [RADIUS_TIMING] [AUTHENTICATION] Request ended at: %s | Duration: %d ms", traceId, endTime, durationMs);

            MDC.remove(AuthServiceConstants.TRACE_ID);
            MDC.remove(AuthServiceConstants.PARAM_USER_NAME);
        }
    }

    private void logPacketReceived(String traceId, InetAddress clientAddress, Packet packet) {
        logger.infof("[%s] Received packet from %s: %s",
                traceId, clientAddress.getHostAddress(), packet.getClass().getSimpleName());
    }

    private Packet handleAccessRequest(String traceId, AccessRequest requestPacket) throws InterruptedException {
        if (isMissingMessageAuthenticator(requestPacket)) {
            logger.warnf("[%s] Rejecting request without Message-Authenticator", traceId);
            return buildAccessReject("Missing Message-Authenticator");
        }

        Optional<UserName> userNameAttr = requestPacket.getAttribute(UserName.class);
        Optional<ChapChallenge> chapChallengeAttribute = requestPacket.getAttribute(ChapChallenge.class);
        Optional<ChapPassword> chapPasswordAttribute = requestPacket.getAttribute(ChapPassword.class);
        Optional<NasIpAddress> nasIpAddressAttribute = requestPacket.getAttribute(NasIpAddress.class);

        if (userNameAttr.isEmpty()) {
            logger.warnf("[%s] Missing required attributes in request", traceId);
            return buildAccessReject("Missing required attributes");
        }

        String username = userNameAttr.get().getData().getValue();
        MDC.put(AuthServiceConstants.PARAM_USER_NAME, username);

        String password = extractUserPassword(requestPacket);
        String chapChallenge = chapChallengeAttribute.map(attr -> bytesToHex(attr.getData().getValue())).orElse(null);
        String chapPassword = chapPasswordAttribute.map(attr -> bytesToHex(attr.getData().getValue())).orElse(null);
        String nasIpAddress = nasIpAddressAttribute
                .map(attr -> ((Inet4Address) attr.getData().getValue()).getHostAddress())
                .orElse(null);

        logger.infof("[%s] Authentication request for user: %s", traceId, username);
        return authenticateUser(traceId, username, password, chapChallenge, chapPassword, nasIpAddress);
    }

    private Packet authenticateUser(String traceId, String username, String password,
                                    String chapChallenge, String chapPassword, String nasIpAddress) throws InterruptedException {

        UserDetails userDetails = authManagementServiceClient.authenticate(username, password, chapChallenge, chapPassword, nasIpAddress);
        logger.infof("[%s] Authorization result for user '%s': %s", traceId, username, userDetails.getIsAuthorized());

        if (!userDetails.getIsEnoughBalance()) {
            logger.warnf("[%s] Authentication failed — user don't have enough quota: %s", traceId, username);
            return buildAccessReject("User don't have enough quota");
        }

        if (!userDetails.getIsActive()) {
            logger.warnf("[%s] Authentication failed — user inactive: %s", traceId, username);
            return buildAccessReject("User is inactive");
        }

        if (userDetails.getIsAuthorized()) {
            logger.infof("[%s] Authentication successful for user: %s", traceId, username);
            return buildAccessAccept(userDetails);
        }

        logger.warnf("[%s] Authentication failed — invalid credentials: %s", traceId, username);
        return buildAccessReject("Invalid username or password");
    }

    private boolean isMissingMessageAuthenticator(Packet packet) {
        return packet.getAttribute(MessageAuthenticator.class).isEmpty();
    }

    private String extractUserPassword(Packet packet) {
        return packet.getAttribute(UserPassword.class)
                .map(attr -> new String(attr.getData().getValue(), StandardCharsets.UTF_8))
                .orElse(null);
    }

    private Packet buildAccessReject(String message) {
        return new AccessReject(List.of(
                new MessageAuthenticator(),
                new ReplyMessage(new TextData(message))
        ));
    }

    private Packet buildAccessAccept(UserDetails userDetails) {
        List<Attribute<?>> attributes = new ArrayList<>();
        attributes.add(new MessageAuthenticator());
        attributes.add(new ReplyMessage(new TextData("Welcome, " + userDetails.getUsername() + "!")));
        attributes.add(new UserName(new TextData(userDetails.getUsername())));

        if (userDetails.getAttributes() != null) {
            userDetails.getAttributes().forEach((key, value) -> {
                switch (key.toUpperCase()) {
                    case "SESSION_TIMEOUT":
                        attributes.add(new SessionTimeout(new IntegerData(Integer.parseInt(value))));
                        break;
                    case "IDLE_TIMEOUT":
                        attributes.add(new IdleTimeout(new IntegerData(Integer.parseInt(value))));
                        break;
                    default:
                        break;
                }
            });
        }
        // Add Nokia vendor-specific attribute
        if (userDetails.getRule() != null) {
            attributes.add(NokiaDictionary.createAlcSubscProfStr(userDetails.getRule()));
        }

        return new AccessAccept(attributes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}