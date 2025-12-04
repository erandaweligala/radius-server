package com.csg.airtel.aaa4j.domain.model;

import java.util.HashMap;
import java.util.Map;
public class UserDetails {
    private String username;
    private boolean isAuthorized;
    private boolean isActive;
    private boolean isEnoughBalance;
    private Map<String, String> attributes;


    // No-args constructor required for JSON serialization/deserialization
    public UserDetails() {
    }

    public UserDetails(String username, boolean isAuthorized, boolean isActive,boolean isEnoughBalance, Map<String, String> attributes) {
        this.username = username;
        this.isAuthorized = isAuthorized;
        this.isActive = isActive;
        this.isEnoughBalance = isEnoughBalance;
        this.attributes = attributes;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean getIsAuthorized() { return isAuthorized; }
    public void setIsAuthorized(boolean isAuthorized) { this.isAuthorized = isAuthorized; }
    public boolean getIsActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }
    public boolean getIsEnoughBalance() { return isEnoughBalance; }
    public void setIsEnoughBalance(boolean isEnoughBalance) { this.isEnoughBalance = isEnoughBalance; }

    public Map<String, String> getAttributes() {
        return attributes != null ? attributes : new HashMap<>();
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }
}
