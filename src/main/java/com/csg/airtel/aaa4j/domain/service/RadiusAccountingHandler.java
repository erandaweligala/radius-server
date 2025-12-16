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
        String traceId = MDC.get("traceId");

        if (logger.isDebugEnabled()) {
            logger.debugf("TraceId : %s Received accounting packet from %s", traceId, clientAddress.getHostAddress());
        }
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

            return publishEventAndCreateResponse(traceId, actionType, commonAttrs, accountingRequest);
        } catch (Exception e) {
            logger.errorf(e, "TraceId : %s Error processing accounting packet from %s",
                    traceId, clientAddress.getHostAddress());
            return null;
        }
    }

    /**
     * Optimized attribute extraction - reduces Optional overhead by direct access
     */
    private CommonAttributes extractCommonAttributes(Packet packet, InetAddress clientAddress) {
        String clientAddressStr = clientAddress.getHostAddress();

        var sessionIdAttr = packet.getAttribute(AcctSessionId.class).orElse(null);
        String sessionId = sessionIdAttr != null ? sessionIdAttr.getData().getValue() : null;

        var nasIpAttr = packet.getAttribute(NasIpAddress.class).orElse(null);
        String nasIp = nasIpAttr != null ? nasIpAttr.getData().getValue().getHostAddress() : clientAddressStr;

        var userNameAttr = packet.getAttribute(UserName.class).orElse(null);
        String userName = userNameAttr != null ? userNameAttr.getData().getValue() : null;

        var nasPortIdAttr = packet.getAttribute(NasPortId.class).orElse(null);
        String nasPortId = nasPortIdAttr != null ? nasPortIdAttr.getData().getValue() : null;

        var nasIdentifierAttr = packet.getAttribute(NasIdentifier.class).orElse(null);
        String nasIdentifier = nasIdentifierAttr != null ? nasIdentifierAttr.getData().getValue() : null;

        var nasPortTypeAttr = packet.getAttribute(NasPortType.class).orElse(null);
        Integer nasPortType = nasPortTypeAttr != null ? nasPortTypeAttr.getData().getValue() : null;

        var delayTimeAttr = packet.getAttribute(AcctDelayTime.class).orElse(null);
        int delayTime = delayTimeAttr != null ? delayTimeAttr.getData().getValue() : 0;

        var eventTimeAttr = packet.getAttribute(EventTimestamp.class).orElse(null);
        Instant eventTime = eventTimeAttr != null ? eventTimeAttr.getData().getValue() : Instant.now();

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

        if (logger.isDebugEnabled()) {
            logger.debugf("[TraceId : %s] START - User: %s, Session: %s, Framed-IP: %s",
                    traceId, common.userName, common.sessionId, framedIp);
        }

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

        // Only log at DEBUG level to reduce overhead
        if (logger.isDebugEnabled()) {
            var inputPacketsAttr = packet.getAttribute(AcctInputPackets.class).orElse(null);
            int inputPackets = inputPacketsAttr != null ? inputPacketsAttr.getData().getValue() : 0;

            var outputPacketsAttr = packet.getAttribute(AcctOutputPackets.class).orElse(null);
            int outputPackets = outputPacketsAttr != null ? outputPacketsAttr.getData().getValue() : 0;

            logger.debugf("[TraceId : %s] INTERIM - User: %s, Session: %s, SessionTime: %ds, " +
                            "Input: %d bytes (%d packets), Output: %d bytes (%d packets)",
                    traceId, common.userName, common.sessionId, sessionTime, inputPackets, outputPackets);
        }

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

        if (logger.isDebugEnabled()) {
            var terminateCauseAttr = packet.getAttribute(AcctTerminateCause.class).orElse(null);
            Integer terminateCause = terminateCauseAttr != null ? terminateCauseAttr.getData().getValue() : null;

            logger.debugf("TraceId : %s STOP - User: %s, Session: %s, SessionTime: %ds, " +
                            "TotalInput: %d bytes, TotalOutput: %d bytes, TerminateCause: %s",
                    traceId, common.userName, common.sessionId, sessionTime, inputOctets, outputOctets,
                    terminateCause != null ? getTerminateCauseDescription(terminateCause) : "Unknown");
        }

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
     * Publishes the accounting event to Kafka and creates the response.
     * Returns null if the publish fails, which signals the NAS to retry.
     */
    private Packet publishEventAndCreateResponse(String traceId, AccountingRequestDto.ActionType actionType,
                                                  CommonAttributes commonAttrs, AccountingRequestDto accountingRequest) {
        try {
            long start = System.currentTimeMillis();
            radiusAccountingProducer.produceAccountingEvent(accountingRequest).toCompletableFuture().join();
            logger.infof("kafka publish complete %s ms", System.currentTimeMillis() - start);
            if (logger.isDebugEnabled()) {
                logger.debugf("[TraceId : %s] Accounting %s processed for user %s, session %s",
                        traceId, actionType, commonAttrs.userName, commonAttrs.sessionId);
            }

            return createAccountingResponse(commonAttrs.sessionId);
        } catch (Exception e) {
            logger.errorf("[TraceId : %s] Accounting event publish failed for session %s, not sending response to NAS",
                    traceId, commonAttrs.sessionId);
            return null;
        }
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