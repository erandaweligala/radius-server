package com.csg.airtel.aaa4j.domain.model;

import java.util.Collections;
import java.util.List;

public class UserRequest {
    private String username;
    private String password;
    private List<String> allowedSsids;

    // No-args constructor required for JSON serialization/deserialization
    public UserRequest() {
    }

    public UserRequest(String username, String password, List<String> allowedSsids) {
        this.username = username;
        this.password = password;
        this.allowedSsids = allowedSsids;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getAllowedSsids() {
        return allowedSsids != null ? allowedSsids : Collections.emptyList();
    }

    public void setAllowedSsids(List<String> allowedSsids) {
        this.allowedSsids = allowedSsids;
    }
}

