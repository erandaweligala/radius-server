package com.csg.airtel.aaa4j.application.config;


import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "radius")
public interface RadiusServerConfig {

    /**
     * Authentication server configuration
     */
    AuthConfig auth();

    /**
     * Accounting server configuration
     */
    AccountingConfig accounting();



    /**
     * Shared configuration
     */
    @WithDefault("sharedsecret")
    String sharedSecret();

    /**
     * Whether to fail application startup if any RADIUS server fails to start
     */
    @WithDefault("true")
    boolean failOnStartupError();

    interface AuthConfig {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("1812")
        int port();

        @WithDefault("0.0.0.0")
        String bindAddress();
    }

    interface AccountingConfig {
        @WithDefault("true")
        boolean enabled();

        @WithDefault("1813")
        int port();

        @WithDefault("127.0.0.1")
        String bindAddress();
    }


}