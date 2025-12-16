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


@ApplicationScoped
public class RadiusAccountingHandler implements RadiusServer.Handler {
    private static final Logger logger = Logger.getLogger(RadiusAccountingHandler.class);

    @ConfigProperty(name = "radius.shared-secret")
    String sharedSecret;

    private final RadiusAccountingProducer radiusAccountingProducer;


    public RadiusAccountingHandler(RadiusAccountingProducer radiusAccountingProducer) {
        this.radiusAccountingProducer = radiusAccountingProducer;
    }

    @Override
    public byte[] handleClient(InetAddress clientAddress) {
        // Return cached shared secret
        return sharedSecret.getBytes(StandardCharsets.UTF_8);
    }


    @Override
    public Packet handlePacket(InetAddress clientAddress, Packet packet) {
        long start = System.currentTimeMillis();
        String traceId = MDC.get("traceId");

        if (!(packet instanceof AccountingRequest)) {
            logger.warnf("TraceId : %s Non-accounting packet received from %s", traceId, clientAddress.getHostAddress());
            return null;
        }

        try {
            var statusOpt = packet.getAttribute(AcctStatusType.class);
            if (statusOpt.isEmpty()) {
                logger.warnf("TraceId : %s Missing AcctStatusType attribute in packet from %s",
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

            Packet response = publishEventAndCreateResponse(traceId, actionType, commonAttrs, accountingRequest);
            logger.infof("complete process account in %d ms", System.currentTimeMillis() - start );
            return response;
        } catch (Exception e) {
            logger.errorf(e, "TraceId : %s Error processing accounting packet from %s",
                    traceId, clientAddress.getHostAddress());
            return null;
        }
    }

    /**
     * Optimized attribute extraction - minimizes Optional overhead and allocations
     */
    private CommonAttributes extractCommonAttributes(Packet packet, InetAddress clientAddress) {
        // Pre-compute client address string to avoid repeated calls
        String clientAddressStr = clientAddress.getHostAddress();

        // Extract session ID (required attribute, fast path)
        String sessionId = packet.getAttribute(AcctSessionId.class)
                .map(attr -> attr.getData().getValue())
                .orElse(null);

        // Extract NAS IP with fallback to client address
        String nasIp = packet.getAttribute(NasIpAddress.class)
                .map(attr -> attr.getData().getValue().getHostAddress())
                .orElse(clientAddressStr);

        // Extract username (required for accounting)
        String userName = packet.getAttribute(UserName.class)
                .map(attr -> attr.getData().getValue())
                .orElse(null);

        // Extract optional port identification attributes
        String nasPortId = packet.getAttribute(NasPortId.class)
                .map(attr -> attr.getData().getValue())
                .orElse(null);

        String nasIdentifier = packet.getAttribute(NasIdentifier.class)
                .map(attr -> attr.getData().getValue())
                .orElse(null);

        Integer nasPortType = packet.getAttribute(NasPortType.class)
                .map(attr -> attr.getData().getValue())
                .orElse(null);

        // Extract timing attributes with sensible defaults
        int delayTime = packet.getAttribute(AcctDelayTime.class)
                .map(attr -> attr.getData().getValue())
                .orElse(0);

        Instant eventTime = packet.getAttribute(EventTimestamp.class)
                .map(attr -> attr.getData().getValue())
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
        var framedIpAttr = packet.getAttribute(FramedIpAddress.class).orElse(null);
        String framedIp = framedIpAttr != null ? framedIpAttr.getData().getValue().getHostAddress() : null;

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
        var framedIpAttr = packet.getAttribute(FramedIpAddress.class).orElse(null);
        String framedIp = framedIpAttr != null ? framedIpAttr.getData().getValue().getHostAddress() : null;

        var inputOctetsAttr = packet.getAttribute(AcctInputOctets.class).orElse(null);
        int inputOctets = inputOctetsAttr != null ? inputOctetsAttr.getData().getValue() : 0;

        var outputOctetsAttr = packet.getAttribute(AcctOutputOctets.class).orElse(null);
        int outputOctets = outputOctetsAttr != null ? outputOctetsAttr.getData().getValue() : 0;

        var sessionTimeAttr = packet.getAttribute(AcctSessionTime.class).orElse(null);
        int sessionTime = sessionTimeAttr != null ? sessionTimeAttr.getData().getValue() : 0;

        var inputGigaWordsAttr = packet.getAttribute(AcctInputGigawords.class).orElse(null);
        int inputGigaWords = inputGigaWordsAttr != null ? inputGigaWordsAttr.getData().getValue() : 0;

        var outputGigaWordsAttr = packet.getAttribute(AcctOutputGigawords.class).orElse(null);
        int outputGigaWords = outputGigaWordsAttr != null ? outputGigaWordsAttr.getData().getValue() : 0;

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
        var inputOctetsAttr = packet.getAttribute(AcctInputOctets.class).orElse(null);
        int inputOctets = inputOctetsAttr != null ? inputOctetsAttr.getData().getValue() : 0;

        var outputOctetsAttr = packet.getAttribute(AcctOutputOctets.class).orElse(null);
        int outputOctets = outputOctetsAttr != null ? outputOctetsAttr.getData().getValue() : 0;

        var sessionTimeAttr = packet.getAttribute(AcctSessionTime.class).orElse(null);
        int sessionTime = sessionTimeAttr != null ? sessionTimeAttr.getData().getValue() : 0;

        var inputGigaWordsAttr = packet.getAttribute(AcctInputGigawords.class).orElse(null);
        int inputGigaWords = inputGigaWordsAttr != null ? inputGigaWordsAttr.getData().getValue() : 0;

        var outputGigaWordsAttr = packet.getAttribute(AcctOutputGigawords.class).orElse(null);
        int outputGigaWords = outputGigaWordsAttr != null ? outputGigaWordsAttr.getData().getValue() : 0;

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

    /**
     * Acknowledges immediately and processes asynchronously with zero overhead.
     * Fire-and-forget pattern - Kafka publish happens in background without blocking.
     */
    private Packet publishEventAndCreateResponse(String traceId, AccountingRequestDto.ActionType actionType,
                                                  CommonAttributes commonAttrs, AccountingRequestDto accountingRequest) {

        radiusAccountingProducer.produceAccountingEvent(accountingRequest)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.errorf(throwable, "[TraceId : %s] Async accounting event publish failed for session %s",
                                traceId, commonAttrs.sessionId);
                    }
                });


        return createAccountingResponse(commonAttrs.sessionId);
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


        /**
         * Inner class to hold common attributes extracted from all accounting packets
         */
        private record CommonAttributes(String clientAddress, String sessionId, String nasIp, String userName,
                                        String nasPortId, String nasIdentifier, Integer nasPortType, int delayTime,Instant eventTime) {
    }
}