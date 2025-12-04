package com.csg.airtel.aaa4j.domain.model;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

public class ClientRequest {

    private String ipAddress;
    private String sharedSecret;

    public ClientRequest() {
        // Required for JSON-B (default constructor)
    }

    @JsonbCreator
    public ClientRequest(
            @JsonbProperty("ipAddress") String ipAddress,
            @JsonbProperty("sharedSecret") String sharedSecret) {
        this.ipAddress = ipAddress;
        this.sharedSecret = sharedSecret;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSharedSecret() {
        return sharedSecret;
    }

    public void setSharedSecret(String sharedSecret) {
        this.sharedSecret = sharedSecret;
    }
}

