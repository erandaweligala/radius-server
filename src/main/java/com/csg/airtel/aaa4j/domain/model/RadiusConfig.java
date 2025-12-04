package com.csg.airtel.aaa4j.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RadiusConfig(
        @JsonProperty("serverAddress") String serverAddress,
        @JsonProperty("port") int port,
        @JsonProperty("sharedSecret") String sharedSecret
){
}
