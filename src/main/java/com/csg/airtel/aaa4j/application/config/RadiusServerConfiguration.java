package com.csg.airtel.aaa4j.application.config;

import com.csg.airtel.aaa4j.common.constant.ResponseCodeEnum;
import com.csg.airtel.aaa4j.domain.service.RadiusAccountingHandler;
import com.csg.airtel.aaa4j.domain.service.RadiusAuthenticationHandler;
import com.csg.airtel.aaa4j.exception.BaseException;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.aaa4j.radius.server.RadiusServer;
import org.aaa4j.radius.server.servers.UdpRadiusServer;
import org.jboss.logging.Logger;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Startup
public class RadiusServerConfiguration {

    //todo implement fixed sonar issues
    private static final Logger logger = Logger.getLogger(RadiusServerConfiguration.class);

    private final RadiusServerConfig config;
    private final RadiusAuthenticationHandler radiusAuthenticationHandler;
    private final RadiusAccountingHandler accountingHandler;

    private RadiusServer radiusServer;
    private RadiusServer accountingServer;

    @Inject
    public RadiusServerConfiguration(
            RadiusServerConfig config,RadiusAuthenticationHandler radiusAuthenticationHandler,
            RadiusAccountingHandler accountingHandler) {
        this.config = config;
        this.radiusAuthenticationHandler = radiusAuthenticationHandler;
        this.accountingHandler = accountingHandler;
    }

    @PostConstruct
    void init() throws BaseException {
        logger.info("Initializing RADIUS servers with configuration...");
        logConfiguration();

        List<String> errors = new ArrayList<>();

        // Start Authentication Server
        if (config.auth().enabled()) {
            try {
                startAuthenticationServer();
            } catch (InterruptedException e) {
                handleServerStartupError("Authentication", e, errors);
            }
        } else {
            logger.info("Authentication RADIUS server is disabled by configuration");
        }

        // Start Accounting Server
        if (config.accounting().enabled()) {
            try {
                startAccountingServer();
            } catch (InterruptedException e) {
                handleServerStartupError("Accounting", e, errors);
            }
        } else {
            logger.info("Accounting RADIUS server is disabled by configuration");
        }

        // Summary
        if (!errors.isEmpty()) {
            logger.warn("RADIUS servers started with {} error(s)");
        } else {
            logger.info("All enabled RADIUS servers started successfully");
        }
    }

    private void logConfiguration() {
        logger.infof("RADIUS Configuration:");
        logger.infof("  Auth Server    : enabled={}, port={}, bind={}",
                config.auth().enabled(), config.auth().port(), config.auth().bindAddress());
        logger.infof("  Accounting     : enabled={}, port={}, bind={}",
                config.accounting().enabled(), config.accounting().port(), config.accounting().bindAddress());
        logger.infof("  Fail on Error  : {}", config.failOnStartupError());
    }

    private void handleServerStartupError(String serverType, Exception e, List<String> errors) throws BaseException {
        String error = String.format("Failed to start %s RADIUS server: %s", serverType, e.getMessage());
        logger.error(error, e);
        errors.add(error);

        if (config.failOnStartupError()) {
            throw new BaseException(
                    error,
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.description(),
                    Response.Status.INTERNAL_SERVER_ERROR,
                    ResponseCodeEnum.EXCEPTION_SERVICE_LAYER.code(),
                    e.getStackTrace()
            );
        }
    }

    private void startAuthenticationServer() throws InterruptedException {
        logger.infof("Starting Authentication RADIUS server on {}:{}",
                config.auth().bindAddress(), config.auth().port());

        radiusServer = UdpRadiusServer.newBuilder()
                .bindAddress(new InetSocketAddress("0.0.0.0", 1812))
                .handler(radiusAuthenticationHandler)
                .build();

        radiusServer.start();
        logger.infof("Authentication RADIUS server started successfully on {}:{}",
                config.auth().bindAddress(), config.auth().port());
    }

    private void startAccountingServer() throws InterruptedException {
        logger.infof("Starting Accounting RADIUS server on {}:{}",
                config.accounting().bindAddress(), config.accounting().port());

        accountingServer = UdpRadiusServer.newBuilder()
                .bindAddress(new InetSocketAddress(config.accounting().bindAddress(), config.accounting().port()))
                .handler(accountingHandler)
                .build();

        accountingServer.start();
        logger.infof("Accounting RADIUS server started successfully on {}:{}",
                config.accounting().bindAddress(), config.accounting().port());
    }

    @PreDestroy
    void shutdown() throws InterruptedException {
        if (radiusServer != null) {
            logger.info("Stopping Authentication RADIUS server...");
            radiusServer.stop();
            logger.info("Authentication RADIUS server stopped");
        }
        if (accountingServer != null) {
            logger.info("Stopping Accounting RADIUS server...");
            accountingServer.stop();
            logger.info("Accounting RADIUS server stopped");
        }
    }
}
