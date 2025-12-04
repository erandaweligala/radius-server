package com.csg.airtel.aaa4j.domain.service;

import com.csg.airtel.aaa4j.domain.model.AccountingResponseEvent;
import com.csg.airtel.aaa4j.domain.model.RadiusConfig;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.aaa4j.radius.core.attribute.Attribute;
import org.aaa4j.radius.core.attribute.Ipv4AddrData;
import org.aaa4j.radius.core.attribute.TextData;
import org.aaa4j.radius.core.attribute.attributes.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@ApplicationScoped
public class ResponseHandler {

    private static final Logger log = Logger.getLogger(ResponseHandler.class);

    private final RadiusClientService radiusClientService;

    @ConfigProperty(name = "client.address")
    String serverAddress;

    @ConfigProperty(name = "client.coa-port")
    int coaPort;

    @ConfigProperty(name = "radius.accounting.port")
    int accountingPort;

    @ConfigProperty(name = "client.shared-secret")
    String sharedSecret;

    @Inject
    public ResponseHandler(RadiusClientService radiusClientService) {
        this.radiusClientService = Objects.requireNonNull(radiusClientService,
                "radiusClientService cannot be null");
        log.info("AccountResponseHandler initialized");
    }

    public Uni<Void> processAccountingResponse(AccountingResponseEvent responseEvent) {
        log.infof("[traceId : %s] Starting processing of accounting response event for sessionId: %s",
                responseEvent.eventId(), responseEvent.sessionId());

        if (log.isDebugEnabled()) {
            log.debugf("Processing accounting response event: eventType=%s, action=%s, sessionId=%s",
                    responseEvent.eventType(),
                    responseEvent.action(),
                    responseEvent.sessionId());
        }

        return Uni.createFrom().item(responseEvent)
                .chain(event -> switch (event.eventType()) {
                    case COA -> handleCoaEvent(event);
                    case CONTINUE -> handleContinueEvent(event);
                    case NO_RESPONSE -> handleNoResponse();
                })
                .onFailure().invoke(e ->
                        log.errorf(e, "Error processing accounting response event for sessionId: %s",
                                responseEvent.sessionId())
                );
    }

    private Uni<Void> handleCoaEvent(AccountingResponseEvent responseEvent) {
        log.info("Handling COA Disconnect event type");

        if (responseEvent.action() == AccountingResponseEvent.ResponseAction.DISCONNECT) {
            log.infof("Initiating COA Disconnect request for sessionId: %s",
                    responseEvent.sessionId());

            return radiusClientService.initiate(
                    buildAttributes(responseEvent.qosParameters()),
                    40,
                    new RadiusConfig(serverAddress, coaPort, sharedSecret)
            );
        } else {
            log.debug("COA action is FUP Apply, COA request");
            return Uni.createFrom().voidItem();
        }
    }

    private Uni<Void> handleContinueEvent(AccountingResponseEvent responseEvent) {
        log.infof("Initiating AccountingResponse for sessionId: %s", responseEvent.sessionId());

        return radiusClientService.initiate(
                buildAccountingAttributes(responseEvent),
                5,
                new RadiusConfig(serverAddress, accountingPort, sharedSecret)
        ).invoke(() ->
                log.infof("[traceId : %s] Complete AccountingResponse for sessionId: %s",
                        responseEvent.eventId(), responseEvent.sessionId())
        );
    }

    private Uni<Void> handleNoResponse() {
        log.errorf("Failed to create AccountingResponse - sessionId is null or blank");
        return Uni.createFrom().voidItem();
    }

    private List<Attribute<?>> buildAccountingAttributes(AccountingResponseEvent responseEvent) {
        List<Attribute<?>> attributes = new ArrayList<>();
        log.infof("Building AccountingResponse attributes for sessionId: %s", responseEvent.sessionId());

        attributes.add(new AcctSessionId(new TextData(responseEvent.sessionId())));
        attributes.add(new ReplyMessage(new TextData(responseEvent.message())));

        return attributes;
    }

    private List<Attribute<?>> buildAttributes(Map<String, String> qosParameters) {
        List<Attribute<?>> attributes = new ArrayList<>();

        qosParameters.forEach((key, value) -> {
            if (value == null || value.trim().isEmpty()) {
                log.infof("Skipping empty value for key: {}", key);
                return;
            }

            if ("username".equalsIgnoreCase(key)) {
                attributes.add(new UserName(new TextData(value)));
                log.infof("Added UserName attribute: {}", value);

            } else if ("sessionId".equalsIgnoreCase(key)) {
                attributes.add(new AcctSessionId(new TextData(value)));
                log.infof("Added AcctSessionId attribute: {}", value);

            } else if ("nasIP".equalsIgnoreCase(key)) {
                parseIpAddress(value).ifPresent(ip -> {
                    attributes.add(new NasIpAddress(new Ipv4AddrData(ip)));
                    log.infof("Added NasIpAddress attribute: {}", value);
                });

            } else if ("framedIP".equalsIgnoreCase(key)) {
                parseIpAddress(value).ifPresent(ip -> {
                    attributes.add(new FramedIpAddress(new Ipv4AddrData(ip)));
                    log.infof("Added FramedIpAddress attribute: {}", value);
                });

            } else {
                log.infof("Unknown QoS parameter: {} = {}", key, value);
            }
        });

        return attributes;
    }

    /**
     * Parse IP address with proper error handling
     */
    private Optional<Inet4Address> parseIpAddress(String ipString) {
        try {
            InetAddress address = InetAddress.getByName(ipString);
            if (address instanceof Inet4Address inet4Address) {
                return Optional.of(inet4Address);
            } else {
                log.infof("Invalid IPv4 address format, expected IPv4 but got IPv6 or invalid format: {}", ipString);
                return Optional.empty();
            }
        } catch (UnknownHostException e) {
            log.error("Failed to parse IP address '{}': {}. COA request will continue without this attribute.",
                    ipString, e);
            return Optional.empty();
        }
    }
}