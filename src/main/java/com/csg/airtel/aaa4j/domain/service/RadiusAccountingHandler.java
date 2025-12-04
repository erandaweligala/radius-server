package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.domain.model.AccountingRequestDto;
import com.csg.airtel.aaa4j.domain.producer.RadiusAccountingProducer;
import jakarta.enterprise.context.ApplicationScoped;
import org.aaa4j.radius.core.attribute.TextData;
import org.aaa4j.radius.core.attribute.attributes.*;
import org.aaa4j.radius.core.packet.Packet;
import org.aaa4j.radius.core.packet.packets.AccountingRequest;
import org.aaa4j.radius.core.packet.packets.AccountingResponse;
import org.aaa4j.radius.server.RadiusServer;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;


@ApplicationScoped
public class RadiusAccountingHandler implements RadiusServer.Handler {
    private static final Logger logger = Logger.getLogger(RadiusAccountingHandler.class);
    private static final int MAX_TPS = 500; // Maximum 500 transactions per second
    private static final long WINDOW_SIZE_MS = 1000; // 1 second window

    @ConfigProperty(name = "radius.shared-secret")
    String sharedSecret;

    private final RadiusAccountingProducer radiusAccountingProducer;

    // Rate limiting using sliding window
    private final ConcurrentLinkedDeque<Long> requestTimestamps = new ConcurrentLinkedDeque<>();
    private final AtomicLong droppedRequestCount = new AtomicLong(0);
    private final AtomicLong totalRequestCount = new AtomicLong(0);

    public RadiusAccountingHandler(RadiusAccountingProducer radiusAccountingProducer) {
        this.radiusAccountingProducer = radiusAccountingProducer;
    }

    @Override
    public byte[] handleClient(InetAddress clientAddress) {
        // Return cached shared secret
        return sharedSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Check if the current request should be rate-limited based on 500 TPS limit
     * Uses a sliding window algorithm to track requests in the last second
     * @return true if request is within limit, false if rate limit exceeded
     */
    private boolean checkRateLimit() {
        long currentTime = System.currentTimeMillis();
        long windowStart = currentTime - WINDOW_SIZE_MS;

        // Remove timestamps outside the current window
        while (!requestTimestamps.isEmpty() && requestTimestamps.peekFirst() < windowStart) {
            requestTimestamps.pollFirst();
        }

        // Check if we're at or over the limit
        if (requestTimestamps.size() >= MAX_TPS) {
            return false; // Rate limit exceeded
        }

        // Add current request timestamp
        requestTimestamps.addLast(currentTime);
        return true; // Within rate limit
    }

    @Override
    public Packet handlePacket(InetAddress clientAddress, Packet packet) {
        String traceId = MDC.get("traceId");
        long totalRequests = totalRequestCount.incrementAndGet();

        // Check rate limit before processing
        if (!checkRateLimit()) {
            long droppedCount = droppedRequestCount.incrementAndGet();
            logger.warnf("[TraceId : %s] RATE_LIMIT_EXCEEDED - Request from %s dropped (Total requests: %d, Dropped: %d, Limit: %d TPS)",
                    traceId, clientAddress.getHostAddress(), totalRequests, droppedCount, MAX_TPS);

            // Log additional details every 100 dropped requests
            if (droppedCount % 100 == 0) {
                logger.errorf("[TraceId : %s] RATE_LIMIT_ALERT - %d requests dropped so far. Current rate exceeds %d TPS limit",
                        traceId, droppedCount, MAX_TPS);
            }

            return null; // Drop the request
        }

        logger.infof("[TraceId : %s] Received accounting packet from %s", traceId, clientAddress.getHostAddress());

        if (!(packet instanceof AccountingRequest)) {
            logger.warnf("[TraceId : %s] Non-accounting packet received from %s", traceId, clientAddress.getHostAddress());
            return null;
        }

        try {
            var statusOpt = packet.getAttribute(AcctStatusType.class);
            if (statusOpt.isEmpty()) {
                logger.warnf("[TraceId : %s] Missing AcctStatusType attribute in packet from %s",
                        traceId, clientAddress.getHostAddress());
                return null;
            }

            AccountingRequestDto.ActionType actionType = determineActionType(statusOpt.get());

            // Extract common attributes
            CommonAttributes commonAttrs = extractCommonAttributes(packet, clientAddress);

            // Extract scenario-specific attributes based on action type
            AccountingRequestDto accountingRequest = switch (actionType) {
                case START -> buildStartRequest(traceId, commonAttrs, packet);
                case INTERIM_UPDATE -> buildInterimRequest(traceId, commonAttrs, packet);
                case STOP -> buildStopRequest(traceId, commonAttrs, packet);
            };

            radiusAccountingProducer.produceAccountingEvent(accountingRequest);

            if (logger.isDebugEnabled()) {
                logger.debugf("[TraceId : %s] Processed accounting %s for session %s from %s",
                        traceId, actionType, commonAttrs.sessionId, commonAttrs.clientAddress);
            }

            logger.infof("[TraceId : %s] Accounting %s processed for user %s",
                    traceId, actionType, commonAttrs.userName);

            return createAccountingResponse(commonAttrs.sessionId);

        } catch (Exception e) {
            logger.errorf(e, "[TraceId : %s] Error processing accounting packet from %s",
                    traceId, clientAddress.getHostAddress());
            return null;
        }
    }

    /**
     * Extract common attributes present in all accounting request types
     */
    private CommonAttributes extractCommonAttributes(Packet packet, InetAddress clientAddress) {
        String clientAddressStr = clientAddress.getHostAddress();

        String sessionId = packet.getAttribute(AcctSessionId.class)
                .map(a -> a.getData().getValue())
                .orElse(null);

        String nasIp = packet.getAttribute(NasIpAddress.class)
                .map(a -> a.getData().getValue().getHostAddress())
                .orElse(clientAddressStr);

        String userName = packet.getAttribute(UserName.class)
                .map(a -> a.getData().getValue())
                .orElse(null);

        String nasPortId = packet.getAttribute(NasPortId.class)
                .map(a -> a.getData().getValue())
                .orElse(null);

        String nasIdentifier = packet.getAttribute(NasIdentifier.class)
                .map(a -> a.getData().getValue())
                .orElse(null);

        Integer nasPortType = packet.getAttribute(NasPortType.class)
                .map(a -> a.getData().getValue())
                .orElse(null);

        int delayTime = packet.getAttribute(AcctDelayTime.class)
                .map(a -> a.getData().getValue())
                .orElse(0);
        Instant eventTime = packet.getAttribute(EventTimestamp.class)
                .map(a -> a.getData().getValue())
                .orElse(Instant.now());


        return new CommonAttributes(
                clientAddressStr,
                sessionId,
                nasIp,
                userName,
                nasPortId,
                nasIdentifier,
                nasPortType,
                delayTime,
                eventTime
        );
    }

    /**
     * Build START accounting request with START-specific attributes
     */
    private AccountingRequestDto buildStartRequest(String traceId, CommonAttributes common, Packet packet) {
        String framedIp = packet.getAttribute(FramedIpAddress.class)
                .map(a -> a.getData().getValue().getHostAddress())
                .orElse(null);

        logger.infof("[TraceId : %s] START - User: %s, Session: %s, Framed-IP: %s, NAS-Port: %s",
                traceId, common.userName, common.sessionId, framedIp, common.nasPortId);

        return new AccountingRequestDto(
                traceId,
                common.sessionId,
                common.nasIp,
                common.userName,
                AccountingRequestDto.ActionType.START,
                0,  // inputOctets - not present in START
                0,  // outputOctets - not present in START
                0,  // sessionTime - not present in START
                common.eventTime,
                common.nasPortId,
                framedIp,
                common.delayTime,
                0,  // inputGigaWords - not present in START
                0,   // outputGigaWords - not present in START
                common.nasIdentifier

        );
    }

    /**
     * Build INTERIM accounting request with INTERIM-specific attributes (usage data)
     */
    private AccountingRequestDto buildInterimRequest(String traceId, CommonAttributes common, Packet packet) {
        String framedIp = packet.getAttribute(FramedIpAddress.class)
                .map(a -> a.getData().getValue().getHostAddress())
                .orElse(null);

        int inputOctets = packet.getAttribute(AcctInputOctets.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int outputOctets = packet.getAttribute(AcctOutputOctets.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int sessionTime = packet.getAttribute(AcctSessionTime.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int inputGigaWords = packet.getAttribute(AcctInputGigawords.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int outputGigaWords = packet.getAttribute(AcctOutputGigawords.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int inputPackets = packet.getAttribute(AcctInputPackets.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int outputPackets = packet.getAttribute(AcctOutputPackets.class)
                .map(a -> a.getData().getValue())
                .orElse(0);


        logger.infof("[TraceId : %s] INTERIM - User: %s, Session: %s, SessionTime: %ds, " +
                        "Input: %d bytes (%d packets), Output: %d bytes (%d packets)",
                traceId, common.userName, common.sessionId, sessionTime
                , inputPackets, outputPackets);

        return new AccountingRequestDto(
                traceId,
                common.sessionId,
                common.nasIp,
                common.userName,
                AccountingRequestDto.ActionType.INTERIM_UPDATE,
                inputOctets,
                outputOctets,
                sessionTime,
                common.eventTime,
                common.nasPortId,
                framedIp,
                common.delayTime,
                inputGigaWords,
                outputGigaWords,
                common.nasIdentifier
        );
    }

    /**
     * Build STOP accounting request with STOP-specific attributes
     */
    private AccountingRequestDto buildStopRequest(String traceId, CommonAttributes common, Packet packet) {
        int inputOctets = packet.getAttribute(AcctInputOctets.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int outputOctets = packet.getAttribute(AcctOutputOctets.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int sessionTime = packet.getAttribute(AcctSessionTime.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int inputGigaWords = packet.getAttribute(AcctInputGigawords.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        int outputGigaWords = packet.getAttribute(AcctOutputGigawords.class)
                .map(a -> a.getData().getValue())
                .orElse(0);

        Integer terminateCause = packet.getAttribute(AcctTerminateCause.class)
                .map(a -> a.getData().getValue())
                .orElse(null);

        logger.infof("[TraceId : %s] STOP - User: %s, Session: %s, SessionTime: %ds, " +
                        "TotalInput: %d bytes, TotalOutput: %d bytes, TerminateCause: %s",
                traceId, common.userName, common.sessionId, sessionTime,
                terminateCause != null ? getTerminateCauseDescription(terminateCause) : "Unknown");

        return new AccountingRequestDto(
                traceId,
                common.sessionId,
                common.nasIp,
                common.userName,
                AccountingRequestDto.ActionType.STOP,
                inputOctets,
                outputOctets,
                sessionTime,
                common.eventTime,
                common.nasPortId,
                null,  // framedIp - not always present in STOP
                common.delayTime,
                inputGigaWords,
                outputGigaWords,
                common.nasIdentifier
        );
    }

    private AccountingRequestDto.ActionType determineActionType(AcctStatusType statusType) {
        int statusValue = statusType.getData().getValue();

        return switch (statusValue) {
            case 1 -> AccountingRequestDto.ActionType.START;
            case 2 -> AccountingRequestDto.ActionType.STOP;
            case 3 -> AccountingRequestDto.ActionType.INTERIM_UPDATE;
            default -> {
                logger.warnf("Unknown Acct-Status-Type: %d, treating as START", statusValue);
                yield AccountingRequestDto.ActionType.START;
            }
        };
    }

    private AccountingResponse createAccountingResponse(String sessionId) {
        if (sessionId != null) {
            return new AccountingResponse(List.of(
                    new ReplyMessage(new TextData("ACKNOWLEDGED")),
                    new AcctSessionId(new TextData(sessionId))
            ));
        }
        return new AccountingResponse(Collections.emptyList());
    }

    private String getTerminateCauseDescription(int cause) {
        return switch (cause) {
            case 1 -> "User-Request";
            case 2 -> "Lost-Carrier";
            case 3 -> "Lost-Service";
            case 4 -> "Idle-Timeout";
            case 5 -> "Session-Timeout";
            case 6 -> "Admin-Reset";
            case 7 -> "Admin-Reboot";
            case 8 -> "Port-Error";
            case 9 -> "NAS-Error";
            case 10 -> "NAS-Request";
            case 11 -> "NAS-Reboot";
            case 12 -> "Port-Unneeded";
            case 13 -> "Port-Preempted";
            case 14 -> "Port-Suspended";
            case 15 -> "Service-Unavailable";
            case 16 -> "Callback";
            case 17 -> "User-Error";
            case 18 -> "Host-Request";
            default -> "Unknown (" + cause + ")";
        };
    }

        /**
         * Inner class to hold common attributes extracted from all accounting packets
         */
        private record CommonAttributes(String clientAddress, String sessionId, String nasIp, String userName,
                                        String nasPortId, String nasIdentifier, Integer nasPortType, int delayTime,Instant eventTime) {
    }
}